package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DataDrivenDatasetReaderTest {
 @Test void readsCsv() throws Exception { Path p=Files.createTempFile("users-",".csv");Files.writeString(p,"id,name\n1,Alice\n2,Bob\n");var rows=new DataDrivenDatasetReader(new ObjectMapper()).read(p);assertEquals(2,rows.size());assertEquals("Alice",rows.get(0).get("name"));Files.deleteIfExists(p); }
 @Test void rejectsDuplicateCsvHeader() throws Exception { Path p=Files.createTempFile("bad-",".csv");Files.writeString(p,"id,id\n1,2\n");var e=assertThrows(AgentExecutionException.class,()->new DataDrivenDatasetReader(new ObjectMapper()).read(p));assertEquals(AgentExecutionException.Category.PLAN_VALIDATION,e.category());Files.deleteIfExists(p); }
 @Test void rejectsColumnMismatch() throws Exception { Path p=Files.createTempFile("bad-",".csv");Files.writeString(p,"id,name\n1\n");assertThrows(AgentExecutionException.class,()->new DataDrivenDatasetReader(new ObjectMapper()).read(p));Files.deleteIfExists(p); }
 @Test void readsJsonArray() throws Exception { Path p=Files.createTempFile("data-",".json");Files.writeString(p,"[{\"id\":\"1\"}]");assertEquals("1",new DataDrivenDatasetReader(new ObjectMapper()).read(p).get(0).get("id"));Files.deleteIfExists(p); }
}
