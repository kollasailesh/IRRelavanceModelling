import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class QueryOperations {
	static ArrayList<String> queries = new ArrayList<String>();
	static double []length = new double [1401];
	static {length[0] = 1;}
	//method to read a query 

	public static void queryDriver(){
		normalizedLength();
		for(String q: queries){
			try {
				queryProcessing(q);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void readQueries(File file) throws IOException{
		if (file != null) {
			try (BufferedReader br = new BufferedReader(new FileReader(file))){
				StringBuffer temp = new StringBuffer();
				for (String line; (line = br.readLine()) != null;) {
					if(!line.startsWith("Q")){
						temp.append(line+" ");
					}
					if(line.length() == 0){
						queries.add(temp.toString());
						temp = new StringBuffer();
					}
				}
				queries.add(temp.toString());
			}
		}
	}
	// method to do query processing
	public static void queryProcessing(String query) throws IOException{
		TreeMap<String, Integer> queryLemmas = new TreeMap<String, Integer>();
		String[] words = query.split(" ");	    //splitting the words separated by whitespace.
		for (String word : words) {
			ArrayList<String> processedToken = new ArrayList<String>();
			processedToken = Tokenizer.processWord(word);
			//Storing the tokens,count in a tree map 
			for (String token : processedToken) {
				if(token.length()<1)
					continue;
				List<String> lemmas = Tokenizer.slt.lemmatize(token);
				for(String lemma : lemmas){
					if (queryLemmas.containsKey(lemma)) {
						int count = queryLemmas.get(lemma);
						count++;
						queryLemmas.put(lemma, count);
					} else {
						queryLemmas.put(lemma, 1);
					}
				}
			}
		}
		queryLemmas = Tokenizer.removeStopWords(queryLemmas);
		System.out.println("______________________________________________________________________________");
		System.out.println("Using Weighting Schema 1");
		System.out.println("______________________________________________________________________________");
		LinkedHashMap <String,Double> top5docs = cosineScoreW1(queryLemmas);
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("For query:"+query);
		System.out.println("Vector representaion of is");
		System.out.println(queryLemmas);
		System.out.println("top5Docs are:");
		System.out.println("Doc"+"\t"+"Rank"+"\t"+"Score"+"\t"+"Headline");
		int rank = 1;
		for(Entry<String, Double> doc:top5docs.entrySet()){
			String docName = "cranfield" + String.format("%04d", Integer.parseInt(doc.getKey()));
			String title = getTitle(docName);
			System.out.println(doc.getKey()+"\t"+rank+"\t"+doc.getValue()+"\t"+title);
			rank++;
		}

		LinkedHashMap <String,Double> top5docsw2 = cosineScoreW2(queryLemmas);
		System.out.println("______________________________________________________________________________");
		System.out.println("Using Weighting Schema 2");
		System.out.println("______________________________________________________________________________");
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("For query:"+query);
		System.out.println("Vector representaion of is");
		System.out.println(queryLemmas);
		System.out.println("top5Docs are:");
		System.out.println("Doc"+"\t"+"Rank"+"\t"+"Score"+"\t"+"Headline");
		int rankw2 = 1;
		for(Entry<String, Double> doc:top5docsw2.entrySet()){
			String docName = "cranfield" + String.format("%04d", Integer.parseInt(doc.getKey()));
			String title = getTitle(docName);
			System.out.println(doc.getKey()+"\t"+rankw2+"\t"+doc.getValue()+"\t"+title);
			rankw2++;
		}
	}

	//method to calculate cosine score for all docs and return top5 docs with weighting scheme 1
	static LinkedHashMap<String, Double> cosineScoreW1(TreeMap<String, Integer> queryLemmas){
		double []scores = new double[Tokenizer.CollectionSize+1];
		scores[0] =0;
		for(Entry<String, Integer> t:queryLemmas.entrySet()){
			double wtq = weight1tq(t.getKey(),queryLemmas);
			if(Tokenizer.lemmaDictionary.containsKey(t.getKey())){
				TermProperties temp = Tokenizer.lemmaDictionary.get(t.getKey());
				for(Entry<String, Integer> file:temp.postingFileInfo.entrySet()){
					double wtd = weight1td(t.getKey(),file.getKey());
					scores[Integer.parseInt(file.getKey().replaceAll("\\D+",""))] += wtd*wtq;
				}
			}
		}
		for(int d =1; d<scores.length;d++){
			scores[d] = scores[d]/length[d];
		}
		LinkedHashMap<String,Double> top5docs = new LinkedHashMap<String,Double>();
		List<Double> unsortedScores = new ArrayList<Double>() ;
		for(int i=0; i<scores.length; i++){
			unsortedScores.add(i, scores[i]);
		}
		List<Double> sortedScores = new ArrayList(unsortedScores);
		Collections.sort(sortedScores, Collections.reverseOrder());
		List<Double> top5 = new ArrayList<Double>(sortedScores.subList(0, 5));
		for(double d: top5){
			double score = d;
			String docNo = Integer.toString(unsortedScores.indexOf(score));
			top5docs.put(docNo,score);
		}
		return top5docs;
	}

	//method to calculate cosine score for all docs and return top5 docs with weighting scheme 1
	static LinkedHashMap<String, Double> cosineScoreW2(TreeMap<String, Integer> queryLemmas){
		double []scores = new double[Tokenizer.CollectionSize+1];
		scores[0] =0;
		for(Entry<String, Integer> t:queryLemmas.entrySet()){
			double wtq = weight2tq(t.getKey(),queryLemmas);
			if(Tokenizer.lemmaDictionary.containsKey(t.getKey())){
				TermProperties temp = Tokenizer.lemmaDictionary.get(t.getKey());
				for(Entry<String, Integer> file:temp.postingFileInfo.entrySet()){
					double wtd = weight2td(t.getKey(),file.getKey());
					scores[Integer.parseInt(file.getKey().replaceAll("\\D+",""))] += wtd*wtq;
				}
			}
		}
		for(int d =1; d<scores.length;d++){
			scores[d] = scores[d]/length[d];
		}
		LinkedHashMap<String,Double> top5docs = new LinkedHashMap<String,Double>();
		List<Double> unsortedScores = new ArrayList<Double>() ;
		for(int i=0; i<scores.length; i++){
			unsortedScores.add(i, scores[i]);
		}
		List<Double> sortedScores = new ArrayList(unsortedScores);
		Collections.sort(sortedScores, Collections.reverseOrder());
		List<Double> top5 = new ArrayList<Double>(sortedScores.subList(0, 5));
		for(double d: top5){
			double score = d;
			String docNo = Integer.toString(unsortedScores.indexOf(score));
			top5docs.put(docNo,score);
		}
		return top5docs;
	}

	//weighting scheme 1 method
	static double weight1td(String term,String doc){
		int df = Tokenizer.lemmaDictionary.get(term).docFrequency;
		int tf = Tokenizer.lemmaDictionary.get(term).postingFileInfo.get(doc);
		String [] docProps= Tokenizer.docProperties.get(doc).split(" ");
		int maxtf = Integer.parseInt(docProps[1]);
		double w1 = (0.4 + 0.6 * Math.log(tf+0.5) / Math.log(maxtf + 1.0))*(Math.log(Tokenizer.CollectionSize/df)/ Math.log((Tokenizer.CollectionSize)));  
		return w1;
	}
	static double weight1tq(String term,TreeMap<String, Integer> queryLemmas){
		int tf = queryLemmas.get(term);
		TreeMap<String, Integer> sortedqueryLemma= (TreeMap<String, Integer>) Tokenizer.sortByValues(queryLemmas);
		int maxtf = sortedqueryLemma.firstEntry().getValue();		
		double w1 = (0.4 + 0.6 * Math.log(tf+0.5) / Math.log(maxtf + 1.0))*1/ Math.log((Tokenizer.CollectionSize));  
		return w1;
	}


	//weigting scheme 2 method
	static double weight2td(String term,String doc){
		int df = Tokenizer.lemmaDictionary.get(term).docFrequency;
		int tf = Tokenizer.lemmaDictionary.get(term).postingFileInfo.get(doc);
		String [] docProps= Tokenizer.docProperties.get(doc).split(" ");
		int doclen = Integer.parseInt(docProps[0]);
		int maxtf = Integer.parseInt(docProps[1]);
		double w2 = (0.4 + 0.6 * (tf / (tf + 0.5 + 1.5 *(doclen /Tokenizer.avgdoclen)))*Math.log(Tokenizer.CollectionSize/df)/Math.log(Tokenizer.CollectionSize));   
		return w2;
	}
	static double weight2tq(String term,TreeMap<String, Integer> queryLemmas){
		int tf =queryLemmas.get(term);
		double w2 = (0.4 + 0.6 * (tf / (tf + 0.5 + 1.5))*1/Math.log(Tokenizer.CollectionSize));   
		return w2;
	}

	static void normalizedLength(){
		for(String doc:Tokenizer.docProperties.keySet()){
			double tempSum = 0;
			for( Entry<String, TermProperties> lemma:Tokenizer.lemmaDictionary.entrySet()){
				if(lemma.getValue().postingFileInfo.containsKey(doc))
					tempSum += Math.pow(weight1td(lemma.getKey(),doc), 2);
			}
			tempSum = Math.sqrt(tempSum);
			length[Integer.parseInt(doc.replaceAll("\\D+",""))]= tempSum;
		}
	}

	static String getTitle(String docName) throws IOException{
		String title = new String();
//		docName = "C:/Users/SAILESH/OneDrive/Studies/4-Fall2015/IR/Cranfield/"+docName;
		docName = "/people/cs/s/sanda/cs6322/Cranfield/"+docName;
		try (BufferedReader br = new BufferedReader(new FileReader(docName))){
			for (String line; (line = br.readLine()) != null;) {
				if(line.equals("<TITLE>")){
					String line2 = br.readLine();
					while(!line2.equals("</TITLE>")){
						title = title+" "+line2;
						line2 = br.readLine();
					}
				}
			}
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return title;
	}

}

