package de.answer.pdf_cli;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@Slf4j
public class PdfCliApplication implements CommandLineRunner {

    private static final String INPUT_PDF = "input.pdf";
    private static final String OUTPUT_PDF = "output.pdf";
    private static final String SHUFFLED_TO_SPLIT_PDF = "shuffled_to_split.pdf";
    private static final String TERMINATING_PROGRAM = "terminating program!";

    public static void main(String[] args) {
        log.info("STARTING THE APPLICATION");
        SpringApplication.run(PdfCliApplication.class, args);
        log.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) throws IOException {
        for (int i = 0; i < args.length; ++i) {
            log.info("args[{}]: {}", i, args[i]);
        }

        String inPdf = INPUT_PDF;
        String outPdf = OUTPUT_PDF;
        if (args.length == 1) {
            inPdf = args[0];
        } else if (args.length == 2) {
            inPdf = args[0];
            outPdf = args[1];
        }

        Path inPdfPath = Paths.get(inPdf);
        PDDocument document;
        if (Files.exists(inPdfPath)) {
            log.info("loading in-pdf: {}", inPdf);
            document = Loader.loadPDF(new File(inPdf));
        } else {
            log.info("in-pdf does not exist!");
            log.info(TERMINATING_PROGRAM);
            return;
        }

        int pageCount = document.getNumberOfPages();
        log.info("page-count: {}", pageCount);
        if (pageCount % 2 != 0) {
            log.info("page-count not dividable by 2");
            log.info(TERMINATING_PROGRAM);
            document.close();
            return;
        }


        PDPageTree pages = document.getDocumentCatalog().getPages();
        if (pages.getCount() >= 4) {
            log.info("more than 3 page found - starting to shuffle!");
            int pageCountDividedByTwo = pageCount / 2;
            log.info("page-count-divided-by-two: {}", pageCountDividedByTwo);
            int i = 0;
            log.info("shuffling pages...");
            while (i < pageCount) {
                int stableIndex = i;
                int moveIndex = stableIndex + pageCountDividedByTwo;
                PDPage movePage = pages.get(moveIndex);
                PDPage stablePage = pages.get(stableIndex);
                pages.insertAfter(movePage, stablePage);
                i += 2;
            }
            log.info("done shuffling!");

            //fail check
            int numberOfPagesAfterShuffle = document.getNumberOfPages();
            int expectedNumberOfPagesAfterShuffle = 3 * pageCountDividedByTwo;
            if (numberOfPagesAfterShuffle != expectedNumberOfPagesAfterShuffle) {
                log.info("number-of-pages-after-shuffle {} not equal expected-number-of-pages-after-shuffle {}!",
                        numberOfPagesAfterShuffle, expectedNumberOfPagesAfterShuffle);
                log.info(TERMINATING_PROGRAM);
                document.close();
                return;
            } else {
                log.info("number-of-pages-after-shuffle: {}", numberOfPagesAfterShuffle);
                log.info("saving shuffled pdf with name: {}", SHUFFLED_TO_SPLIT_PDF);
                document.save(SHUFFLED_TO_SPLIT_PDF);
                document.close();
            }

            log.info("loading pdf to split with name {}", SHUFFLED_TO_SPLIT_PDF);
            PDDocument rDocument = Loader.loadPDF(new File(SHUFFLED_TO_SPLIT_PDF));

            Splitter splitter = new Splitter();
            splitter.setStartPage(1);
            splitter.setEndPage(pageCount);
            splitter.setSplitAtPage(pageCount + 1);
            log.info("splitting pdf...");
            List<PDDocument> splitDocuments = splitter.split(rDocument);
            log.info("done splitting!");

            for (PDDocument splitDocument : splitDocuments) {

                Path outPdfPath = Paths.get(outPdf);
                if (Files.exists(outPdfPath)) {
                    log.info("out-pdf already exists!");
                    log.info("deleting already existing out-pdf with name {}", outPdf);
                    Files.delete(outPdfPath);
                }

                log.info("saving splitted pdf with name {}", outPdf);
                splitDocument.save(outPdf);
                splitDocument.close();
            }

            rDocument.close();

            log.info("deleting shuffled pdf with name {}", SHUFFLED_TO_SPLIT_PDF);
            Files.delete(Paths.get(SHUFFLED_TO_SPLIT_PDF));
        } else {
            log.info("in-pdf has to few pages: {}", pageCount);
            log.info(TERMINATING_PROGRAM);
        }
    }
}
