import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WordCounter {
    public static final Path FOLDER_OF_TEXT_FILES  = Paths.get("");
    public static final Path WORD_COUNT_TABLE_FILE = Paths.get("");
    public static final int  NUMBER_OF_THREADS     = 2;
    //Note: there's this one random nullpointerexception i believe i fixed, but im not too sure. Essentially, every 40
    //runs, it would throw nullpointer for trying to access an element in the hashmap, that exists. Ran it 100 times
    //after my fix, and it didn't show up, so I think it is fixed. :)

    static Queue<Path> files = new LinkedList<>();
    static int fileNum;
    static HashMap<String, Integer[]> results;
    static int numText;
    static List<Thread> threads;
    static DirectoryStream<Path> paths;

    public static void main(String... args) throws IOException {
        fileNum = 0;
        if(!Files.isDirectory(FOLDER_OF_TEXT_FILES) || !FOLDER_OF_TEXT_FILES.toFile().exists()){
            throw new IOException(String.format("Unable to read from directory: %s", FOLDER_OF_TEXT_FILES));
        }
        if(NUMBER_OF_THREADS < 1) {
            throw new IllegalArgumentException("Number of threads must be greater than or equal to 1");
        }
        try {
            paths = Files.newDirectoryStream(FOLDER_OF_TEXT_FILES);
        }
        catch(NoSuchFileException e){
            throw new IOException(String.format("Unable to read from directory: %s", FOLDER_OF_TEXT_FILES));
        }
        if(!WORD_COUNT_TABLE_FILE.toString().endsWith(".txt")){
            throw new IOException("Invalid path to output!");
        }
        for(Path egg: paths){
            if(!egg.equals(WORD_COUNT_TABLE_FILE) && egg.toString().endsWith(".txt")){
                files.add(egg);
            }
        }
        numText = files.size();
        results = new HashMap<>();
        threads = new ArrayList<>();
        for(int i = 0; i < NUMBER_OF_THREADS; i++){
            threads.add(new ReadThread());
            threads.get(i).start();
        }
        for(Thread t : threads){
            try{
                t.join();
            }
            catch (InterruptedException e){
            }
        }
        sumRows();
        String table = makeTable();
        File file = WORD_COUNT_TABLE_FILE.toFile();
        if(!file.exists()){
            file.createNewFile();
        }
        BufferedWriter written = new BufferedWriter(new FileWriter(file));
        written.write(table);
        written.close();
    }
    public static synchronized int incNum() {fileNum++; return fileNum-1;}
    public static synchronized Path getNext() {return files.poll();}
    public static synchronized boolean goOn() {return files.isEmpty();}

    static class ReadThread extends Thread {
        public void run() {
            while(!files.isEmpty()) {
                if(goOn()){interrupt();}
                int fileN = incNum();
                Path filePath = getNext();
                if(filePath != null) {
                    String line = "";
                    try {
                        BufferedReader rea = new BufferedReader(new FileReader(filePath.toString()));
                        line = rea.readLine().trim();
                        rea.close();
                    } catch (IOException e) {
                    }
                    readAdd(line, fileN);
                }
            }
        }
    }

    public static synchronized void addword(String wor) {
        if(!results.containsKey(wor)) {
            Integer[] arr = new Integer[numText + 1];
            Arrays.fill(arr, 0);
            results.put(wor.toLowerCase(), arr);
        }
    }

    public static void readAdd(String lone, int number){
        String line = lone.toLowerCase();
        line = line.replaceAll("\\s{2,}", " ");
        line = line.replaceAll("[:;.,!?]","" );
        String[] words = line.split(" ");
        for (String word : words) {

            addword(word);

            results.get(word)[number] = results.get(word)[number] + 1;
        }
    }

    public static void sumRows(){
        int num = 0;
        Set<String> row = results.keySet();
        for(String word : row){
            for(int i = 0; i <= numText; i++){
                num += results.get(word)[i];
            }
            results.get(word)[numText] = num;
            num = 0;
        }
    }

    public static String makeTable() throws IOException {
        String table = "           ";
        DirectoryStream<Path> path2ElectricBoogaloo = Files.newDirectoryStream(FOLDER_OF_TEXT_FILES);
        for(Path pat : path2ElectricBoogaloo){
            if(!pat.equals(WORD_COUNT_TABLE_FILE) && pat.toString().endsWith(".txt")){
                table = table + String.format("%-10s",pat.getName(pat.getNameCount()-1).
                        toString().replace(".txt", ""));
            }
        }
        table = table + "total\n";
        TreeSet<String> words = new TreeSet<>(results.keySet());
        for(String word: words){
            table = table + String.format("%-10.10s%s", word, " ");
            for(int i = 0; i < numText; i++){
                table = table + String.format("%-10d", results.get(word.toLowerCase())[i]);
            }
            table = table + results.get(word)[numText]+ "\n";
        }
        return table;
    }
}