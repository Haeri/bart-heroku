package net.bart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.tzemp.config.Config;
import com.github.tzemp.config.ParsingRule;
import com.github.tzemp.parser.Parser;
import com.github.tzemp.parser.ParserSummary;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class main {
    public static void main(String[] args){
        System.out.println("Hello World!");

        Config c = readConfig();
        List<String> lines = new ArrayList<String>();

        try {
            lines = FileUtils.readLines(new File("src/main/resources/logs/20181213-183200.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Parser p = new Parser(lines, c);
        ParserSummary ps = p.getParserSummary();

        System.out.println("Build Status: " + ps.getBuildStatus());
        System.out.println("Error cause: " + ps.getErrorCause());
        System.out.println("Question: " + ps.getBestQuestion());
    }

    private static List<String> readLogs(String filePath){
        try{
            Scanner s = new Scanner(new File(filePath));
            List<String> list = new ArrayList<>();
            while (s.hasNext()){
                list.add(s.next());
            }
            s.close();
            return list;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
}