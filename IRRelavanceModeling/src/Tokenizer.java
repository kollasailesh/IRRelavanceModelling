import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;	
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.IIOException;
/*
Name: Sailesh KOlla
Netid: sxk145331

CS6322.501: Information Retrieval
HomeWork 1
This is the Tokenizer class java file

 */
public class Tokenizer {
	static StanfordLemmatizer slt = new StanfordLemmatizer();
	static TreeMap<String,String> docProperties = new TreeMap<String,String>();
	static TreeMap<String,TermProperties> stemDictionary = new TreeMap<String,TermProperties>();
	static TreeMap<String,TermProperties> lemmaDictionary = new TreeMap<String,TermProperties>();
	static HashSet<String> stopWords = new HashSet<>();
	static HashSet<String> specificWords = new HashSet<>();
	static HashSet<String> specificstems = new HashSet<>();
	static HashSet<String> specificlemmas = new HashSet<>();
	static double avgdoclen =0; 
	static int CollectionSize = 0 ;
	public static void main(String[] args) throws IOException {
//		final File folder = new File("C:/Users/SAILESH/OneDrive/Studies/4-Fall2015/IR/Cranfield");
		final File folder = new File("/people/cs/s/sanda/cs6322/Cranfield");
//		readWords(new File("C:/Users/SAILESH/OneDrive/Studies/4-Fall2015/IR/stopwords"), stopWords);
		readWords(new File("/people/cs/s/sanda/cs6322/resourcesIR/stopwords"), stopWords);
//		QueryOperations.readQueries(new File("C:/Users/SAILESH/OneDrive/Studies/4-Fall2015/IR/hw3.queries"));
		QueryOperations.readQueries(new File("/people/cs/s/sanda/cs6322/hw3.queries"));
		try {
			CollectionSize = listFilesForFolder(folder);
			avgdoclen = avgDocLength();
			QueryOperations.queryDriver();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	//	method to read stop words
	private static void readWords(File file,HashSet<String> words) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			for (String line; (line = br.readLine()) != null;) {
				words.add(line.toString().toLowerCase());
			}
		}
	}

	//	 for listing out files in a folder
	public static int listFilesForFolder(final File folder) throws FileNotFoundException {

		for (final File fileEntry : folder.listFiles()) {
			// if the fileEtry is a directory - make recursive call
			if (fileEntry.isDirectory()) {
				CollectionSize += listFilesForFolder(fileEntry);
			}else {// if the fileEntry is not a directory then reading the file
				try {
					readFile(fileEntry);		// line by line.
					CollectionSize ++;
				} catch (IOException e) {
					e.printStackTrace();		// Handling any exceptions
				}
			}
		}
		return CollectionSize;
	}

	// method to read a file tokenize stem and lemmatize the terms.
	private static void readFile(File fileEntry) throws IOException {
		if (fileEntry != null) {
			try (BufferedReader br = new BufferedReader(new FileReader(fileEntry))) {
				TreeMap<String, Integer> tokenCollection = new TreeMap<>();
				TreeMap<String, Integer> stemCollection = new TreeMap<>();
				TreeMap<String, Integer> lemmaCollection = new TreeMap<>();
				for (String line; (line = br.readLine()) != null;) {
					String noHtmlLine = line.toString().replaceAll("\\<.*?>", "");
					if (noHtmlLine.length() < 1) {
						continue;
					}
					noHtmlLine = noHtmlLine.replaceAll("\\'s", "");	// replace 's with null 
					if (noHtmlLine != null) {
						String[] words = noHtmlLine.split(" ");	    //splitting the words separated by whitespace.
						for (String word : words) {
							ArrayList<String> processedToken = new ArrayList<String>();
							processedToken = processWord(word);
							//Storing the tokens,count in a tree map 
							for (String token : processedToken) {
								if(token.length()<1)
									continue;
								if (tokenCollection.containsKey(token)) {
									int count = tokenCollection.get(token);
									count++;
									tokenCollection.put(token, count);
								} else {
									tokenCollection.put(token, 1);
								}
								String stem = stemming(token);
								if (stemCollection.containsKey(stem)) {
									int count = stemCollection.get(stem);
									count++;
									stemCollection.put(stem, count);
								} else {
									stemCollection.put(stem, 1);
								}
								List<String> lemmas = slt.lemmatize(token);
								for(String lemma : lemmas){
									if (lemmaCollection.containsKey(lemma)) {
										int count = lemmaCollection.get(lemma);
										count++;
										lemmaCollection.put(lemma, count);
									} else {
										lemmaCollection.put(lemma, 1);
									}
								}
							}
						}
					}
				}
				br.close();
				int doclen = collectionCount(stemCollection);
				stemCollection = removeStopWords(stemCollection);
				lemmaCollection = removeStopWords(lemmaCollection);
				int maxFreq = maxFrequency(stemCollection);
				String abc = doclen+" "+maxFreq;
				docProperties.put(fileEntry.getName(),abc);
				globalStore(stemCollection,lemmaCollection,fileEntry.getName());
			}
		}
	}
	//method to find avg doc length
	public static double avgDocLength(){
		double avg = 0, sum = 0; 
		for(Entry<String, String> d:docProperties.entrySet()){
			String[] docProp = d.getValue().split(" ");
			sum += Integer.parseInt(docProp[0]);
		}
		avg = sum/CollectionSize;
		return avg;
	}
	//	method to store the lemmas and stems in a global treemap
	private static void globalStore(TreeMap<String, Integer> stemcollection,TreeMap<String, Integer> lemmacollection, String file) {
		//		long stemStartUnCompressTime = System.currentTimeMillis();
		for(Entry<String, Integer> entry: stemcollection.entrySet()){
			if(stemDictionary.containsKey(entry.getKey())){
				TermProperties temp = stemDictionary.get(entry.getKey());
				temp.setDocFrequency(temp.getDocFrequency()+1);
				HashMap<String, Integer> tempMap = temp.getPostingFileInfo();
				tempMap.put(file, entry.getValue());
				temp.setPostingFileInfo(tempMap);
				stemDictionary.put(entry.getKey(), temp);
			}else{
				TermProperties temp = new TermProperties();
				temp.setDocFrequency(1);
				temp.getPostingFileInfo().put(file, entry.getValue());
				stemDictionary.put(entry.getKey(), temp);
			}
		}
		for(Entry<String, Integer> entry: lemmacollection.entrySet()){
			if(lemmaDictionary.containsKey(entry.getKey())){
				TermProperties temp = lemmaDictionary.get(entry.getKey());
				temp.setDocFrequency(temp.getDocFrequency()+1);
				HashMap<String, Integer> tempMap = temp.getPostingFileInfo();
				tempMap.put(file, entry.getValue());
				temp.setPostingFileInfo(tempMap);
				lemmaDictionary.put(entry.getKey(), temp);
			}else{
				TermProperties temp = new TermProperties();
				temp.setDocFrequency(1);
				temp.getPostingFileInfo().put(file, entry.getValue());
				lemmaDictionary.put(entry.getKey(), temp);
			}
		}
	}

	//	method to remove stop words from the treeMap collection
	static TreeMap<String, Integer> removeStopWords(TreeMap<String, Integer> collection) {
		for(String set:stopWords){
			if(collection.containsKey(set)){
				collection.remove(set);
			}
		}
		return collection;
	}
	//	method to tokenize words
	static ArrayList<String> processWord(String word) {
		ArrayList<String> tokens = new ArrayList<String>();
		word = word.toLowerCase();
		if(word.contains("[\\-]")){
			word.replaceAll("[\\-]", "\t").trim();
			if(word.contains("\t")){
				String[] xyz = word.split("\t");
				for(String abc:xyz){
					abc = abc.replaceAll("[\\W+]", "").trim();
					abc = abc.replaceAll("[\\d+]","").trim();
					tokens.add(abc);
				}
			}
		}
		else {
			word = word.replaceAll("[\\W+]", "").trim();
			word =word.replaceAll("[\\d+]","").trim();
			tokens.add(word);
		}
		return tokens;
	}

	//stemmer method call
	@SuppressWarnings("null")
	private static String stemming(String token) {
		Stemmer stm = new Stemmer();
		char [] temp = token.toCharArray();
		for(int j=0; j <temp.length; j++){
			stm.add(temp[j]);
		}
		stm.stem();
		String stemResult = stm.toString();
		return stemResult;
	}

	//method to find total number of tokens
	private static int collectionCount(TreeMap<String, Integer> collection) {

		int count = 0;
		for(Entry<String, Integer> key : collection.entrySet()){
			int i = key.getValue();
			count = count+i;
		}
		return count;
	}


	//Method to return max frequency term and frequency. 
	public static int maxFrequency(TreeMap<String, Integer> collection){
		TreeMap<String, Integer> sortedTree = new TreeMap<String, Integer>();
		sortedTree = (TreeMap<String, Integer>) sortByValues(collection);
		Entry<String, Integer> temp = sortedTree.firstEntry();
		return temp.getValue();
	}

	// over writing comparator to sort tree map by values
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				if (compare == 0)
					return 1;
				else
					return compare;
			}
		};
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}

}
