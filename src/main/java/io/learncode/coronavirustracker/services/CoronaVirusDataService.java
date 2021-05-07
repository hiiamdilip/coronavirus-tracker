package io.learncode.coronavirustracker.services;

import io.learncode.coronavirustracker.models.LocationStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CoronaVirusDataService {

    private static String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";

    private List<LocationStats> allStats = new ArrayList<>();

    public List<LocationStats> getAllStats() {
        return allStats;
    }

    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() throws IOException, InterruptedException {
        List<LocationStats> newStats = new ArrayList<>();
        Map<String, List<Integer>> countryCasesMap = new HashMap<>();
       
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        StringReader csvBodyReader = new StringReader(httpResponse.body());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);
        
        for (CSVRecord record : records) {
        	List<Integer> listOflast2DaysCases = new ArrayList<Integer>();
        	
            if(countryCasesMap.containsKey(record.get("Country/Region"))) {
            	listOflast2DaysCases.add(countryCasesMap.get(record.get("Country/Region")).get(0) + Integer.parseInt(record.get(record.size() - 1)));
            	listOflast2DaysCases.add(countryCasesMap.get(record.get("Country/Region")).get(1) + Integer.parseInt(record.get(record.size() - 2)));
            	countryCasesMap.put(record.get("Country/Region"), listOflast2DaysCases);
            }else {
            	listOflast2DaysCases.add(Integer.parseInt(record.get(record.size() - 1)));
            	listOflast2DaysCases.add(Integer.parseInt(record.get(record.size() - 2)));
            	countryCasesMap.put(record.get("Country/Region"), listOflast2DaysCases);
            } 
        }
        
        for(Map.Entry<String, List<Integer>> entry: countryCasesMap.entrySet()) {
        	LocationStats locationStat = new LocationStats();
        	locationStat.setCountry(entry.getKey());
        	locationStat.setLatestTotalCases(entry.getValue().get(0));
        	locationStat.setDiffFromPrevDay(entry.getValue().get(0) - entry.getValue().get(1));
        	newStats.add(locationStat);
        }
        
        this.allStats = newStats;
    }
}