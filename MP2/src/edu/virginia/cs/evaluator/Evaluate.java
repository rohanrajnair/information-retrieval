package edu.virginia.cs.evaluator;
import edu.virginia.cs.index.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import edu.virginia.cs.index.similarities.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;

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
		else if(method.equals("--dp"))
			searcher.setSimilarity(new DirichletPrior());
		else if(method.equals("--jm"))
			searcher.setSimilarity(new JelinekMercer());
		else if(method.equals("--ok"))
			searcher.setSimilarity(new OkapiBM25());
		else if(method.equals("--pl"))
			searcher.setSimilarity(new PivotedLength());
		else if(method.equals("--tfidf"))
			searcher.setSimilarity(new TFIDFDotProduct());
		else if(method.equals("--bdp"))
			searcher.setSimilarity(new BooleanDotProduct());
		else
		{
			System.out.println("[Error]Unknown retrieval function specified!");
			printUsage();
			System.exit(1);
		}
	}

	public static void printUsage() {
		System.out.println("To specify a ranking function, make your last argument one of the following:");
		System.out.println("\t--dp\tDirichlet Prior");
		System.out.println("\t--jm\tJelinek-Mercer");
		System.out.println("\t--ok\tOkapi BM25");
		System.out.println("\t--pl\tPivoted Length Normalization");
		System.out.println("\t--tfidf\tTFIDF Dot Product");
		System.out.println("\t--bdp\tBoolean Dot Product");
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
		while ((line = br.readLine()) != null) {
			judgement = br.readLine();

			//compute corresponding AP
			meanAvgPrec += AvgPrec(line, judgement);
			//compute corresponding P@K
			p_k += Prec(line, judgement, k);
			//compute corresponding MRR
			mRR += RR(line, judgement);
			//compute corresponding NDCG
			nDCG += NDCG(line, judgement, k);

			++numQueries;
		}
		br.close();

//		try
//		{
//			String filename= "/Users/rohannair/Desktop/dp_map_output.txt";
//			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
//			fw.write("\nmu: " + this.mu);
//			fw.write("\nMAP: " + meanAvgPrec / numQueries);
//			fw.write("\n");
//			fw.close();
//		}
//		catch(IOException ioe)
//		{
//			System.err.println("IOException: " + ioe.getMessage());
//		}


		System.out.println("\nMAP: " + meanAvgPrec / numQueries);//this is the final MAP performance of your selected ranker
		System.out.println("\nP@" + k + ": " + p_k / numQueries);//this is the final P@K performance of your selected ranker
		System.out.println("\nMRR: " + mRR / numQueries);//this is the final MRR performance of your selected ranker
		System.out.println("\nNDCG: " + nDCG / numQueries); //this is the final NDCG performance of your selected ranker
	}

	double AvgPrec(String query, String docString) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i = 1;
		double avgp = 0.0;
		double numRel = 0;
		System.out.println("\nQuery: " + query);
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				//how to accumulate average precision (avgp) when we encounter a relevant document
				numRel ++;
				avgp += numRel/i;
				System.out.print("  ");
			} else {
				//how to accumulate average precision (avgp) when we encounter an irrelevant document
				System.out.print("X ");
			}
			System.out.println(i + ". " + rdoc.title());
			++i;
		}

		//compute average precision here
		if (numRel != 0) avgp = avgp / relDocs.size();

		System.out.println("Average Precision: " + avgp);
		return avgp;
	}

	//precision at K
	double Prec(String query, String docString, int k) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		double p_k = 0;
		double numRel = 0;
		ResultDoc rdoc;
		//your code for computing precision at K here
		int i;
		for (i = 1; i <= k; i++) {
			rdoc = results.get(i-1);
			if (relDocs.contains(rdoc.title())) numRel ++;
		}

		p_k = numRel / (i-1);
		return p_k;
	}

	//Reciprocal Rank
	double RR(String query, String docString) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i = 1;
		double rr = 0;

		ResultDoc rdoc = results.get(i-1);
		//your code for computing Reciprocal Rank here
		while ((!(relDocs.contains(rdoc.title()))) && (i < results.size())) {
			++i;
			rdoc = results.get(i-1);
		}

		if (i == results.size()) rr = 0;
		else rr = 1.0 / i;

		return rr;
	}

	//Normalized Discounted Cumulative Gain
	double NDCG(String query, String docString, int k) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i, rk = 0, numRel = 0;
		double dcg = 0;
		double idcg = 0;
		double ndcg = 0;

		//your code for computing Normalized Discounted Cumulative Gain here
		ResultDoc rdoc;
		//calculate DCG
		for (i = 1; i <= k; ++i) {
			rdoc = results.get(i-1);
			if (relDocs.contains(rdoc.title())) dcg += 1 / (Math.log(i+1)/Math.log(2));
		}

		//calculate IDCG
		if (relDocs.size() >= k) rk = k;
		else rk = relDocs.size();

		int j;
		for (j = 1; j <= rk; j++) {
			idcg += 1 / (Math.log(j+1)/Math.log(2));
		}

		if (idcg != 0) ndcg = dcg / idcg;
		else ndcg = 0;

		return ndcg;
	}
}