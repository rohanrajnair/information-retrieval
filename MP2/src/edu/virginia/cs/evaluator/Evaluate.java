package edu.virginia.cs.evaluator;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.lang.Math;

import org.apache.commons.math3.util.Precision;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.commons.math3.*;
import edu.virginia.cs.index.ResultDoc;
import edu.virginia.cs.index.Searcher;

public class Evaluate {
	/**
	 * Format for judgements.txt is:
	 * 
	 * line 0: <query 1 text> line 1: <space-delimited list of relevant URLs>
	 * line 2: <query 2 text> line 3: <space-delimited list of relevant URLs>
	 * ...
	 * Please keep all these constants!
	 */


	Searcher _searcher = null;

	public static void setSimilarity(Searcher searcher, String method) {
        if(method == null)
            return;
        else if(method.equals("--ok"))
            searcher.setSimilarity(new BM25Similarity());       
        else if(method.equals("--tfidf"))
            searcher.setSimilarity(new DefaultSimilarity());
        else
        {
            System.out.println("[Error]Unknown retrieval function specified!");
            printUsage();
            System.exit(1);
        }
    }
    
    public static void printUsage()
    {
        System.out.println("To specify a ranking function, make your last argument one of the following:");        
        System.out.println("\t--ok\tOkapi BM25");
        System.out.println("\t--tfidf\tTFIDF Dot Product");
    }
    
	//Please implement P@K, MRR and NDCG accordingly
	public void evaluate(String method, String indexPath, String judgeFile) throws IOException {		
		_searcher = new Searcher(indexPath);		
		setSimilarity(_searcher, method);
		
		BufferedReader br = new BufferedReader(new FileReader(judgeFile));
		String line = null, judgement = null;
		int k = 10;
		double meanAvgPrec = 0.0, p_k = 0.0, mRR = 0.0, nDCG = 0.0;
		double numQueries = 0.0;
		File file_map = new File("/Users/rohannair/Desktop/map_bm25.csv");
		BufferedWriter bf_map = new BufferedWriter(new FileWriter(file_map));
		File file_p_k = new File("/Users/rohannair/Desktop/p_k_bm25.csv");
		BufferedWriter bf_p_k = new BufferedWriter(new FileWriter(file_p_k));
		File file_mrr = new File("/Users/rohannair/Desktop/mrr_bm25.csv");
		BufferedWriter bf_mrr = new BufferedWriter(new FileWriter(file_mrr));
		File file_ndcg = new File("/Users/rohannair/Desktop/ndcg_bm25.csv");
		BufferedWriter bf_ndcg = new BufferedWriter(new FileWriter(file_ndcg));
		bf_map.write("Avg Prec");
		bf_map.newLine();
		bf_p_k.write("P@K");
		bf_p_k.newLine();
		bf_mrr.write("RR");
		bf_mrr.newLine();
		bf_ndcg.write("NDCG");
		bf_ndcg.newLine();
		while ((line = br.readLine()) != null) {
			judgement = br.readLine();
			
			//compute corresponding AP
			meanAvgPrec += AvgPrec(line, judgement);
			double tmp = AvgPrec(line, judgement);
			tmp = Precision.round(tmp, 6);
			bf_map.write(String.valueOf(tmp));
			bf_map.newLine();
			//compute corresponding P@K
			p_k += Prec(line, judgement, k);
			bf_p_k.write(String.valueOf(Prec(line, judgement, k)));
			bf_p_k.newLine();
			//compute corresponding MRR
			mRR += RR(line, judgement);
			bf_mrr.write(String.valueOf(RR(line, judgement)));
			bf_mrr.newLine();
			//compute corresponding NDCG
			nDCG += NDCG(line, judgement, k);
			bf_ndcg.write(String.valueOf(NDCG(line, judgement, k)));
			bf_ndcg.newLine();
			++numQueries;
		}
		bf_map.flush();
		bf_mrr.flush();
		bf_p_k.flush();
		bf_ndcg.flush();

		bf_map.close();
		bf_mrr.close();
		bf_p_k.close();
		bf_ndcg.close();
		br.close();
		System.out.println();
		System.out.println("\nMAP: " + meanAvgPrec / numQueries);//this is the final MAP performance of your selected ranker
		System.out.println("\nP@" + k + ": " + p_k / numQueries);//this is the final P@K performance of your selected ranker
		System.out.println("\nMRR: " + mRR / numQueries);//this is the final MRR performance of your selected ranker
		System.out.println("\nNDCG: " + nDCG / numQueries); //this is the final NDCG performance of your selected ranker
		System.out.println(meanAvgPrec);
		System.out.println(numQueries);


	}

	double AvgPrec(String query, String docString) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		double i = 1;
		double avgp = 0.0;
		double numRel = 0; // number of relevant results returned
		//int totRel = relDocs.size(); // total number of relevant documents


		System.out.println("\nQuery: " + query);
		System.out.println("Average Precision: ");
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				// evaluating precision at every recall point (when relevant doc encountered)
				++ numRel;
				avgp += (numRel/i);
				System.out.print("  ");
			} else {
				System.out.print("X ");
			}
			System.out.println(i + ". " + rdoc.title());
			++i;
		}

		if (numRel == 0){
			avgp = 0;
		}
		else{
			avgp /= relDocs.size();
		}
		

		System.out.println("Average Precision: " + avgp);
		System.out.println("Total relevant docs: " + relDocs.size());
		return avgp;
	}
	
	//precision at K
	double Prec(String query, String docString, int k) {
		double p_k = 0;
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		List<ResultDoc> k_results = new ArrayList<ResultDoc>(results.subList(0,k)); // grabbing first k results
		int i = 1;
//		System.out.println("\nQuery: " + query);
//		System.out.println("P@K: ");
		for (ResultDoc rdoc : k_results){
			if (relDocs.contains(rdoc.title())) {
				++ p_k; // counting relevant docs in first k results
//				System.out.print("  ");
			}
			else{
//				System.out.print("X ");
			}
//			System.out.println(i + ". " + rdoc.title());
			++i;
		}

		p_k = p_k/k;
//		System.out.println("P@K: " + p_k);
		return p_k;
	}
	
	//Reciprocal Rank
	double RR(String query, String docString) {
		double rr = 0;
		double i = 1;
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();

		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		boolean flag = false;
//		System.out.println("\nQuery: " + query);
//		System.out.println("RR: ");
		for (ResultDoc rdoc : results){
			if (relDocs.contains(rdoc.title())){
				rr = 1/i; // iterating over results until relevant doc encountered
				flag = true;
//				System.out.print("  ");
			}
			else{
//				System.out.print("X ");
			}
//			System.out.println(i + ". " + rdoc.title());
			if (flag == true){
				break;
			}
			++i;
		}
//		System.out.println("RR: " + rr);
		return rr;
	}
	
	//Normalized Discounted Cumulative Gain
	double NDCG(String query, String docString, int k) {
		double ndcg = 0;
		double dcg = 0;

		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		if (results.size() == 0)
			return 0; // no result returned

		int i = 0;
//		System.out.println("\nQuery: " + query);
//		System.out.println("NDCG");
		for (ResultDoc rdoc : results){
			double curr = 0;
			if (i == k){ // nDCG @ k, only need first k results
				break;
			}
			if (relDocs.contains(rdoc.title())){
				double denom = Math.log(2+i)/Math.log(2); // calculating denominator
				curr = 1/denom;
//				System.out.print("  ");
			}
			else{
//				System.out.print("X "); // irrelevant docs do not contribute to gain
			}
//			System.out.println(i + ". " + rdoc.title());
			++i;
			dcg += curr;
		}
		double groundTruth = 0;
		for (int j = 0; j < k; ++j){ // computing ideal DCG
			if (j == relDocs.size()){ // checking if fewer than k relevant docs
				break;
			}
			double denom = Math.log(2+j)/Math.log(2); // calculating denominator
			double curr = 1/denom;
			groundTruth += curr;
		}

		ndcg = dcg/groundTruth; // divide by iDCG
//		System.out.println("NDCG: " + ndcg);

		return ndcg;
	}
}