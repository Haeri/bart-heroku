package net.bart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.tzemp.config.Config;
import com.github.tzemp.parser.Parser;
import com.github.tzemp.parser.ParserSummary;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import spark.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import static spark.Spark.*;
import static spark.debug.DebugScreen.*;

public class Main {
    private static final String UPLOAD_DIR = "upload";
    public static void main(String[] args){
        port(getHerokuAssignedPort());
        //enableDebugScreen();

        File uploadDir = new File(UPLOAD_DIR);
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

        staticFiles.externalLocation(UPLOAD_DIR);

        get("/", (req, res) -> {
            String ret = "";
            File lastFile = lastFileModified(UPLOAD_DIR);

            if(lastFile == null){
                return "<h1>No Data</h1>";
            }

            Config c = readConfig();
            List<String> lines = new ArrayList<String>();

            try {
                lines = FileUtils.readLines(lastFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Parser p = new Parser(lines, c);
            ParserSummary ps = p.getParserSummary();


            ret += "<h1>Last log at: " + lastFile.lastModified() + "</h1>";
            ret += "<h2>BART outputs:</h2>";

            ret += "Build Status: " + ps.getBuildStatus() + "<br>";
            ret += "Error cause: " + ps.getErrorCause() + "<br>";
            ret += "Question: " + ps.getBestQuestion() + "<br>";

            return ret;
        });

        post("/", (req, res) -> {
            //String ret = "";
            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
                logInfo(req, tempFile);
                return "success!";
            }catch (Exception e){
                return e.getStackTrace().toString();
            }
        });
    }
    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    private static Config readConfig() {
        System.out.println("START READING CONFIG");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Config config = mapper.readValue(new File("src/main/resources/config/config.yaml"), Config.class);
            System.out.println("END READING CONFIG - SUCCESS");
            return config;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("END READING CONFIG - FAILURE");
        return null;
    }


    // methods used for logging
    private static void logInfo(Request req, Path tempFile) throws IOException, ServletException {
        System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile.toAbsolutePath() + "'");
    }

    private static String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private static File lastFileModified(String dir) {
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }
}