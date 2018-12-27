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

    public static void main(String[] args){
        port(getHerokuAssignedPort());
        enableDebugScreen();

        File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

        staticFiles.externalLocation("upload");

        get("/", (req, res) ->
                "<form method='post' enctype='multipart/form-data'>" // note the enctype
                        + "    <input type='file' name='uploaded_file' accept='.txt'>" // make sure to call getPart using the same "name" in the post
                        + "    <button>Upload Logs</button>"
                        + "</form>"
        );

        post("/", (req, res) -> {
            String ret = "";
            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logInfo(req, tempFile);






            ret += "Hello World!<br>";

            Config c = readConfig();
            List<String> lines = new ArrayList<String>();

            try {
                lines = FileUtils.readLines(tempFile.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Parser p = new Parser(lines, c);
            ParserSummary ps = p.getParserSummary();

            ret += "Build Status: " + ps.getBuildStatus() + "<br>";
            ret += "Error cause: " + ps.getErrorCause() + "<br>";
            ret += "Question: " + ps.getBestQuestion() + "<br>";

            ret += "<h1>You uploaded this image:<h1><img src='" + tempFile.getFileName() + "'>";
            return ret;

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
}