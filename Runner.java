package ie.atu.sw;

import java.util.*;
import java.io.*;

public class Runner {

    //map to store word embeddings
    private static Map<String, double[]> embeddings = new HashMap<>();
    
    //default output file path
    private static String outputFile = "./out.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            displayMenu();
            
            int option = scanner.nextInt();
            scanner.nextLine(); 

            switch (option) {
                case 1:
                    specifyEfile(scanner); //specify embeddings file
                    break;
                case 2:
                    specifyOfile(scanner); //specify output file
                    break;
                case 3:
                    enterWord(scanner); 
                    break;
                case 4:
                    matches(scanner);
                    break;
                case 0:
                    System.exit(0); //quit
                    break;
                default:
                	
                    System.out.println("Invalid option");
            }
        }
    }

    //menu options
    private static void displayMenu() {
        System.out.println("************************************************************");
        System.out.println("*     ATU - Dept. of Computer Science & Applied Physics    *");
        System.out.println("*                                                          *");
        System.out.println("*          Similarity Search with Word Embeddings          *");
        System.out.println("*                                                          *");
        System.out.println("************************************************************");
        System.out.println("(1) Specify Embedding File");
        System.out.println("(2) Specify an Output File (default: ./out.txt)");
        System.out.println("(3) Enter a Word or Text");
        System.out.println("(4) Number of matches");
        System.out.println("(0) Quit");
        System.out.print("Select Option [1-?]> ");
    }

    // search for match words based on input
    private static void matches(Scanner scanner)
    {
        System.out.println("Enter the number of high scoring matches to report:");
        
        int n = scanner.nextInt();
        scanner.nextLine(); 
        
        System.out.println("Enter a word or text: ");
        
        String text = scanner.nextLine();
        searchSimilarity(text, n);
    }

    //enter word method
    private static void enterWord(Scanner scanner)
    {
        System.out.println("Enter a word or text: ");
        String text = scanner.nextLine();
        
        if (!text.isEmpty()) 
        {
            searchSimilarity(text);
        } 
        else 
        {
            System.out.println("No text given");
        }
    }


    private static void specifyOfile(Scanner scanner) 
    {
    	
        System.out.println("Enter the path for the output file (default: ./out.txt):");
        
        String filePath = scanner.nextLine();
        outputFile = filePath.isEmpty() ? "./out.txt" : filePath;
        System.out.println("Output file path set to: " + outputFile);
    }

    // specify the embedding file
    private static void specifyEfile(Scanner scanner)
    {
        System.out.println("Enter the path for the word embeddings file:");
        
        String filePath = scanner.nextLine();
        System.out.println("File path entered: " + filePath); // debug
        
        try
        {
            embeddings = buildEmbeddingsMap(filePath);
            System.out.println("Word embeddings file loaded successfully.");
        } 
        
        catch (IOException e) 
        {
            System.out.println("Error loading word embeddings file: " + e.getMessage());
        }
    }

    // build the embeddings map from the file
    private static Map<String, double[]> buildEmbeddingsMap(String filePath) throws IOException 
    {
        Map<String, double[]> embeddings = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) 
        {
            String line;
            
            while ((line = reader.readLine()) != null) 
            {
                String[] parts = line.trim().split(",\\s*");
                String word = parts[0].trim();
                double[] embedding = new double[parts.length - 1];
                
                for (int i = 1; i < parts.length; i++)
                {
                    embedding[i - 1] = Double.parseDouble(parts[i].trim());
                }
                
                embeddings.put(word, embedding);
            }
        } 
        
        catch (IOException | NumberFormatException e) 
        {
            throw new IOException("Error parsing word embeddings file: " + e.getMessage());
        }
        return embeddings;
    }

    //  search for similar words based on user input
    private static void searchSimilarity(String text) {
        if (embeddings.isEmpty()) {
            System.out.println("Embeddings not loaded. Please specify embedding file first.");
            return;
        }
        
        double[] targetEmbedding = embeddings.getOrDefault(text.toLowerCase(), null);
        
        if (targetEmbedding == null)
        {
            System.out.println("Word not found in embeddings.");
            return;
        }
        
        System.out.println("Searching for similar words for: " + text);
        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        embeddings.forEach((word, embedding) ->
        {
            if (!word.equalsIgnoreCase(text))
            {
                double similarity = cosineSimilarity(targetEmbedding, embedding);
                pq.offer(new AbstractMap.SimpleEntry<>(word, similarity));
            }
        });
        printTopSimilarWords(pq);
    }

    private static void searchSimilarity(String text, int n) 
    {
        if (embeddings.isEmpty()) 
        {
            System.out.println("Embeddings not loaded. Please specify embedding file first.");
            return;
        }
        
        double[] targetEmbedding = embeddings.get(text);
        
        if (targetEmbedding == null) 
        {
            System.out.println("Word not found in embeddings.");
            return;
        }
        
        System.out.println("Searching for similar words for: " + text);
        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        embeddings.forEach((word, embedding) ->
        {
            if (!word.equals(text)) 
            {
                double similarity = cosineSimilarity(targetEmbedding, embedding);
                pq.offer(new AbstractMap.SimpleEntry<>(word, similarity));
            }
        });
        
        printTopNSimilarWords(pq, n);
    }

    //  print top similar words
    private static void printTopSimilarWords(PriorityQueue<Map.Entry<String, Double>> pq) 
    {
        System.out.println("Top 10 similar words:");
        pq.stream().limit(10).forEach(entry -> System.out.println(entry.getKey() + " - Cosine Similarity: " + entry.getValue()));
    }

    // print top 'n' similar words
    private static void printTopNSimilarWords(PriorityQueue<Map.Entry<String, Double>> pq, int n) 
    {
        System.out.println("Top " + n + " similar words:");
        int count = 0;
        
        while (!pq.isEmpty() && count < n)
        {
            Map.Entry<String, Double> entry = pq.poll();
            System.out.println(entry.getKey() + " - Cosine Similarity: " + entry.getValue());
            count++;
        }
    }

    // cosine similarity 
    private static double cosineSimilarity(double[] vectorA, double[] vectorB)
    {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++)
        {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

