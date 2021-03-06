package com.nc.tradox.api;

import com.nc.tradox.model.Country;
import com.nc.tradox.model.Document;
import com.nc.tradox.model.User;
import com.nc.tradox.model.impl.CountryImpl;
import com.nc.tradox.model.impl.Documents;
import com.nc.tradox.model.impl.FullRouteImpl;
import com.nc.tradox.service.TradoxService;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/docs")
@CrossOrigin("*")
public class FillDocsController {

    private final TradoxService tradoxService;

    @Autowired
    public FillDocsController(TradoxService tradoxService) {
        this.tradoxService = tradoxService;
    }

    @PostMapping("/fill")
    public String fillDocs(@RequestBody RouteCredentials fullRoute, BindingResult bindingResult, HttpSession session) throws InvalidFormatException, IOException {
        if (!bindingResult.hasErrors()) {
            int userId = (int) session.getAttribute("userId");
            User user = tradoxService.getUserById(userId);
            Country departure = new CountryImpl(fullRoute.getDepartureId(), null);
            Country destination = new CountryImpl(fullRoute.getDestinationId(), null);
            Documents documents = tradoxService.getDocuments(new FullRouteImpl(departure, destination));
            List<Document> docs = documents.getList();
            System.out.println(docs.size());
            Map<String, XWPFDocument> mapDocs = new HashMap<>();
            for (Document doc : docs) {
                System.out.println(doc.getName());
                System.out.println(doc.getFileLink());
                XWPFDocument docx = fillFile(doc.getFileLink(), user, fullRoute);
                if (docx != null) {
                    mapDocs.put(doc.getName(), docx);
                }
            }
            session.setAttribute("documents", mapDocs);
        }
        return showPdf(session);
    }

    public String showPdf(HttpSession session) throws IOException {
        Map<String, XWPFDocument> docs = (Map<String, XWPFDocument>) session.getAttribute("documents");
        if (docs != null) {
            PdfOptions options = PdfOptions.create();
            for (Map.Entry<String, XWPFDocument> entry : docs.entrySet()) {
                System.out.println(entry);
                PdfConverter.getInstance().convert(entry.getValue(), new FileOutputStream(entry.getKey()), options);
                PDDocument document = PDDocument.load(new File(entry.getKey()));
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(
                            page, 300, ImageType.RGB);
                    ImageIOUtil.writeImage(
                            bim, "image.png", 300);
                }
                document.close();
            }
            return "{\"res\": \"true\"}";
        }
        return "{\"res\": \"false\"}";
    }

    @GetMapping("/pdf")
    public String getPdf(HttpSession session) throws IOException {
        Map<String, XWPFDocument> docs = (Map<String, XWPFDocument>) session.getAttribute("documents");
        String json = "{\"res\":\"false\"}";
        if (docs == null) {
            return json;
        }
        PdfOptions options = PdfOptions.create();
        for (Map.Entry<String, XWPFDocument> entry : docs.entrySet()) {
            PdfConverter.getInstance().convert(entry.getValue(), new FileOutputStream(entry.getKey()), options);
        }
        json = json.replace("false", "true");
        return json;
    }

    private XWPFDocument fillFile(String path, User user, RouteCredentials fullRoute) {
        XWPFDocument docx = null;
        try {
            docx = new XWPFDocument(OPCPackage.open(path));
            for (XWPFTable tbl : docx.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                String text = r.getText(0);
                                if (text != null) {
                                    text = text.replace("country", tradoxService.getCountryById(fullRoute.getDestinationId()).getFullName());
                                    text = text.replace("name", user.getFirstName() + ' ' + user.getLastName());
                                    text = text.replace("(birth)", String.valueOf(user.getBirthDate()));
                                    text = text.replace("(passport code)", user.getPassport().getPassportId());
                                    r.setText(text, 0);
                                }
                            }
                        }
                    }
                }
            }
            //docx.write(new FileOutputStream("output.docx"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
        return docx;
    }
}
