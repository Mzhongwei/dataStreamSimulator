package com.example.dataStreamSimulator.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class readCSV implements CommandLineRunner{
    private static final Logger logger = LoggerFactory.getLogger(readCSV.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${csv.file.path}")
    private String pathCSV;

    @Value("${csv.file.timeout}")
    private int timeout;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    public void run(String... args){
        checkPath(pathCSV);
    }

    private void readData(String pathCSV) throws FileNotFoundException, IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(pathCSV))) {
            String line;
            line = br.readLine();
            String[] headersValues = line.split(",");
            while ((line = br.readLine()) != null) {
                logger.info("[begin] start putting data into kafka producer...");
                ObjectNode jsonvalue = objectMapper.createObjectNode();
                String[] values = line.split(",");
                for(int j=0; j<values.length; j++){
                    jsonvalue.put(headersValues[j], values[j]);
                }
                logger.debug("[sent] json object to kafka producer: " + jsonvalue);
                kafkaProducerService.sendMessage(jsonvalue);
                
                // ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                // scheduler.scheduleAtFixedRate(() -> {
                //     kafkaProducerService.sendMessage(jsonvalue);
                // }, 0, 1, TimeUnit.SECONDS);

                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread sleep interrupted!", e);
                }

            }
            logger.info("[finished] CSV file reading: " + pathCSV + " !");
        }
    }

    private void checkPath(String pathCSV){
        try{
            File f = new File(pathCSV);
            if(f.isFile()){
                readData(pathCSV);
            }else if(f.isDirectory()){
                File[] files = f.listFiles(File::isFile);
                if(files != null){
                    for(File file: files){
                        String path = file.getAbsolutePath();
                        readData(path);
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("Error occured for file " + pathCSV + ": ", e);
        }
    }


}
