package com.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end walk through the main user stories against an in-memory database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:taskflow-test;DB_CLOSE_DELAY=-1",
        "app.storage.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class TaskFlowApiTest {

    private static final String PASSWORD = "Passw0rd!23";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teamCanPlanAndTrackWork() throws Exception {
        String ownerToken = register("Asha Owner", "asha@example.com");
        String memberToken = register("Ravi Member", "ravi@example.com");
        long memberId = json(send(get("/api/users/me"), memberToken, null, 200)).get("id").asLong();

        // owner creates a project, member joins with the invite code
        JsonNode project = json(send(post("/api/projects"), ownerToken,
                Map.of("name", "Website revamp", "description", "Q4 work"), 201));
        long projectId = project.get("id").asLong();
        String inviteCode = project.get("inviteCode").asText();
        send(post("/api/projects/join"), memberToken, Map.of("inviteCode", inviteCode), 200);

        // owner creates a task assigned to the member
        String due = LocalDate.now().plusDays(3).toString();
        JsonNode task = json(send(post("/api/projects/" + projectId + "/tasks"), ownerToken,
                Map.of("title", "Design landing page", "description", "Hero section and footer",
                        "dueDate", due, "priority", "HIGH", "assigneeId", memberId), 201));
        long taskId = task.get("id").asLong();
        assertEquals("OPEN", task.get("status").asText());

        // the member sees it in "my tasks", searches for it, and completes it
        assertEquals(1, json(send(get("/api/tasks/mine"), memberToken, null, 200)).get("totalElements").asInt());
        assertEquals(1, json(send(get("/api/projects/" + projectId + "/tasks?q=footer"), ownerToken, null, 200))
                .get("totalElements").asInt());
        send(patch("/api/tasks/" + taskId + "/status"), memberToken, Map.of("status", "COMPLETED"), 200);

        assertEquals(0, json(send(get("/api/projects/" + projectId + "/tasks?status=OPEN"), ownerToken, null, 200))
                .get("totalElements").asInt());
        assertEquals(1, json(send(get("/api/projects/" + projectId + "/tasks?status=COMPLETED"), ownerToken, null, 200))
                .get("totalElements").asInt());

        // collaboration: comment + attachment
        send(post("/api/tasks/" + taskId + "/comments"), memberToken, Map.of("content", "Mockups are ready"), 201);
        assertEquals(1, json(send(get("/api/tasks/" + taskId + "/comments"), ownerToken, null, 200)).size());

        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        MvcResult upload = mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments").file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isCreated())
                .andReturn();
        long attachmentId = json(upload).get("id").asLong();
        MvcResult download = send(get("/api/tasks/" + taskId + "/attachments/" + attachmentId), ownerToken, null, 200);
        assertEquals("hello", download.getResponse().getContentAsString());

        // validation errors come back as 400 with field details
        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        // only the creator or project owner may delete
        send(delete("/api/tasks/" + taskId), memberToken, null, 403);
        send(delete("/api/tasks/" + taskId), ownerToken, null, 204);
        send(get("/api/tasks/" + taskId), ownerToken, null, 404);
    }

    @Test
    void outsidersAndLoggedOutTokensAreRejected() throws Exception {
        String ownerToken = register("Meera Owner", "meera@example.com");
        String outsiderToken = register("Kabir Outsider", "kabir@example.com");

        long projectId = json(send(post("/api/projects"), ownerToken, Map.of("name", "Private"), 201)).get("id").asLong();

        // no token -> 401, non-member -> 403
        mockMvc.perform(get("/api/projects/" + projectId + "/tasks")).andExpect(status().isUnauthorized());
        send(get("/api/projects/" + projectId + "/tasks"), outsiderToken, null, 403);

        // after logout the same token no longer works
        send(post("/api/auth/logout"), outsiderToken, null, 204);
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isUnauthorized());

        // wrong password -> 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "meera@example.com", "password", "nope-nope"))))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("fullName", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("token").asText();
    }

    private MvcResult send(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
