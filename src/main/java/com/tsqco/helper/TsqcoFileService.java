package com.tsqco.helper;

import org.springframework.stereotype.Service;

import java.io.*;

import static com.tsqco.constants.TsqcoConstants.BASE_DIR;

@Service
public class TsqcoFileService {

    public void writeToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();  // Handle the exception appropriately
        }
    }

    public String readFromFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();  // Handle the exception appropriately
        }
        return content.toString();
    }
}
