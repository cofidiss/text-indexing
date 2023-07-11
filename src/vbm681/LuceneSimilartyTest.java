package vbm681;



import edu.wisc.ischool.wiscir.examples.BM25SimilarityOriginal;
import edu.wisc.ischool.wiscir.utils.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;




import org.apache.commons.compress.utils.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Farkli similarty algoritmalarina gore query sonuclari gosteren siniftir.
 *
 * @author Sinan Balli
 * 
 */

public class LuceneSimilartyTest {
	
static Similarity similarty;
static float lambda ;
public static void CreateIndex(int caseNo) {


	  try {
		  
		  switch (caseNo) {
		  case 1:
			  similarty = new CustomSimilarty(0.1f);
			  
			  break;
		  
		  case 2:
			  similarty = new BooleanSimilarity();
			  
			  break;
		  case 3:
			  similarty = new LMJelinekMercerSimilarity(lambda);
			  break;
		  case 4:
			  similarty = new BM25SimilarityOriginal();
			  break;
			  default:
				  throw new Exception("Yanlis case sayisi girildi.Lütfen 1 ile 3 arasi bir sayi giriniz.");
		  
		  }

          // change the following input and output paths to your local ones
          String pathCorpus =".\\corpus\\example_corpus.gz";
          String pathIndex = String.format(".\\index\\%s", similarty.getClass().getSimpleName().toString());

          Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

          // Analyzer specifies options for text tokenization and normalization (e.g., stemming, stop words removal, case-folding)
          Analyzer analyzer = new Analyzer() {
              @Override
              protected TokenStreamComponents createComponents( String fieldName ) {
                  // Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
                  TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                  // Step 2: transforming all tokens into lowercased ones (recommended for the majority of the problems)
                  ts = new TokenStreamComponents( ts.getSource(), new LowerCaseFilter( ts.getTokenStream() ) );
                  // Step 3: whether to remove stop words (unnecessary to remove stop words unless you can't afford the extra disk space)
                  // Uncomment the following line to remove stop words
                  // ts = new TokenStreamComponents( ts.getSource(), new StopFilter( ts.getTokenStream(), EnglishAnalyzer.ENGLISH_STOP_WORDS_SET ) );
                  // Step 4: whether to apply stemming
                  // Uncomment one of the following two lines to apply Krovetz or Porter stemmer (Krovetz is more common for IR research)
                  ts = new TokenStreamComponents( ts.getSource(), new KStemFilter( ts.getTokenStream() ) );
                  // ts = new TokenStreamComponents( ts.getSource(), new PorterStemFilter( ts.getTokenStream() ) );
                  return ts;
              }
          };

          IndexWriterConfig config = new IndexWriterConfig( analyzer );
          // Note that IndexWriterConfig.OpenMode.CREATE will override the original index in the folder
          config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
          // Lucene's default BM25Similarity stores document field length using a "low-precision" method.
          // Use the BM25SimilarityOriginal to store the original document length values in index.
          config.setSimilarity( similarty );

          IndexWriter ixwriter = new IndexWriter( dir, config );

          // This is the field setting for metadata field (no tokenization, searchable, and stored).
          FieldType fieldTypeMetadata = new FieldType();
          fieldTypeMetadata.setOmitNorms( true );
          fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
          fieldTypeMetadata.setStored( true );
          fieldTypeMetadata.setTokenized( false );
          fieldTypeMetadata.freeze();

          // This is the field setting for normal text field (tokenized, searchable, store document vectors)
          FieldType fieldTypeText = new FieldType();
          fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
          fieldTypeText.setStoreTermVectors( true );
          fieldTypeText.setStoreTermVectorPositions( true );
          fieldTypeText.setTokenized( true );
          fieldTypeText.setStored( true );
          fieldTypeText.freeze();

          // You need to iteratively read each document from the example corpus file,
          // create a Document object for the parsed document, and add that
          // Document object by calling addDocument().

          // Well, the following only works for small text files. DO NOT follow this part for large dataset files.
          InputStream instream = new GZIPInputStream( new FileInputStream( pathCorpus ) );
          String corpusText = new String( IOUtils.toByteArray( instream ), "UTF-8" );
          instream.close();

          Pattern pattern = Pattern.compile(
                  "<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TITLE>(.+?)</TITLE>.+?<AUTHOR>(.+?)</AUTHOR>.+?<SOURCE>(.+?)</SOURCE>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
                  Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL
          );

          Matcher matcher = pattern.matcher( corpusText );

          while ( matcher.find() ) {

              String docno = matcher.group( 1 ).trim();
              String title = matcher.group( 2 ).trim();
              String author = matcher.group( 3 ).trim();
              String source = matcher.group( 4 ).trim();
              String text = matcher.group( 5 ).trim();

              // Create a Document object
              Document d = new Document();
              // Add each field to the document with the appropriate field type options
              d.add( new Field( "docno", docno, fieldTypeMetadata ) );
              d.add( new Field( "title", title, fieldTypeText ) );
              d.add( new Field( "author", author, fieldTypeText ) );
              d.add( new Field( "source", source, fieldTypeText ) );
              d.add( new Field( "text", text, fieldTypeText ) );
              // Add the document to the index
              //System.out.println( "indexing document " + docno );
              ixwriter.addDocument( d );
          }

          // remember to close both the index writer and the directory
          ixwriter.close();
          dir.close();

      } catch ( Exception e ) {
          e.printStackTrace();
      }
  }

public static void Search() {


	String  pathIndex = String.format("..\\vbm681\\index\\%s", similarty.getClass().getSimpleName().toString());
		  


	
	try {

   


        // Analyzer specifies options for text tokenization and normalization (e.g., stemming, stop words removal, case-folding)
        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents( String fieldName ) {
                // Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
                TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                // Step 2: transforming all tokens into lowercased ones (recommended for the majority of the problems)
                ts = new TokenStreamComponents( ts.getSource(), new LowerCaseFilter( ts.getTokenStream() ) );
                // Step 3: whether to remove stop words (unnecessary to remove stop words unless you can't afford the extra disk space)
                // Uncomment the following line to remove stop words
                // ts = new TokenStreamComponents( ts.getSource(), new StopFilter( ts.getTokenStream(), EnglishAnalyzer.ENGLISH_STOP_WORDS_SET ) );
                // Step 4: whether to apply stemming
                // Uncomment one of the following two lines to apply Krovetz or Porter stemmer (Krovetz is more common for IR research)
                ts = new TokenStreamComponents( ts.getSource(), new KStemFilter( ts.getTokenStream() ) );
                // ts = new TokenStreamComponents( ts.getSource(), new PorterStemFilter( ts.getTokenStream() ) );
                return ts;
            }
        };

        String field = "text"; // the field you hope to search for
        QueryParser parser = new QueryParser( field, analyzer ); // a query parser that transforms a text string into Lucene's query object

        String qstr = "query reformulation"; // this is the textual search query
        Query query = parser.parse( qstr ); // this is Lucene's query object

        // Okay, now let's open an index and search for documents
        Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
        IndexReader index = DirectoryReader.open( dir );

        // you need to create a Lucene searcher
        IndexSearcher searcher = new IndexSearcher( index );

        // make sure the similarity class you are using is consistent with those being used for indexing
        searcher.setSimilarity( similarty );

        int top = 10; // Let's just retrieve the talk 10 results
        TopDocs docs = searcher.search( query, top ); // retrieve the top 10 results; retrieved results are stored in TopDocs

        System.out.printf( "%-10s%-25s%-20s%-10s%s\n", "Rank",  "Document Length","DocNo", "Score", "Title");
        int rank = 1;
        for ( ScoreDoc scoreDoc : docs.scoreDocs ) {
            int docid = scoreDoc.doc;
            double score = scoreDoc.score;
            String docno = LuceneUtils.getDocno( index, "docno", docid );
            String title = LuceneUtils.getDocno( index, "title", docid );
            int doclen = 0;
            TermsEnum termsEnum = index.getTermVector( LuceneUtils.findByDocno( index, "docno", docno ), "text" ).iterator();
            while ( termsEnum.next() != null ) {
                doclen += termsEnum.totalTermFreq();
            }
            System.out.printf( "%-10d%-25d%-20s%-10.4f%s\n", rank, doclen,docno, score, title );
            rank++;
        }

        // remember to close the index and the directory
        index.close();
        dir.close();

    } catch ( Exception e ) {
        e.printStackTrace();
    }

}
	
public static void main( String[] args )  {
    	
    	int choose = 0 ;
    	 BufferedReader reader = new BufferedReader(
    	            new InputStreamReader(System.in));
    	
    	        // Reading data using readLine
    	 boolean success = false;
    	      while (!success) {
    	        try {
    	        	 System.out.print("1 ile 4 arasi bir sayi giriniz:\n1: CustomSimilarty Sinifi\n2: BooleanSimilarity Sinifi\n3: LMJelinekMercerSimilarity Sinifi\n4: BM25SimilarityOriginal Sinifi\n");
    	        	choose = Integer.parseInt(reader.readLine());
    	            if (!(0< choose && choose < 5)){
    	            	
    	            	System.err.println("lütfen 1 ile 4 arasi bir sayi giriniz.\n");
    	            	continue;
    	            }
    	            if (choose == 3) {
    	            	System.out.print("LMJelinekMercerSimilarity sinifi icin Lambda parametresi giriniz:\n");
    	            	 lambda = Float.parseFloat(reader.readLine());
    	            	if (!(lambda >0 && lambda <=1 )) {
    	            		System.out.print("Lambda parametresini (0,1] arasi giriniz:\n");
    	            		continue;
    	            	}
    	            
    	            }
    	            success = true;
    	        } 
    	            
    	        catch(Exception e) {
    	            System.out.println("Lütfen bir sayi giriniz!\n");
    	         
    	        }
    	      }
    	   
    	CreateIndex(choose);
    	
Search();
    	
      
}
    
} 