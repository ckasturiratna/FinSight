package com.example.finsight_backend.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailjetEmailServiceTest {

    @Mock
    private MailjetClient mailjetClient;

    @Mock
    private MailjetResponse mailjetResponse;

    private MailjetEmailService mailjetEmailService;

    @BeforeEach
    void setUp() {
        mailjetEmailService = new MailjetEmailService("test", "test");
        mailjetEmailService.setMailjetClient(mailjetClient);
    }

    @Test
    void sendEmailBuildsMailjetRequest() throws Exception {
        when(mailjetClient.post(any(MailjetRequest.class))).thenReturn(mailjetResponse);
        when(mailjetResponse.getStatus()).thenReturn(200);

        mailjetEmailService.sendEmail(
                "test@example.com",
                "Hello",
                "Plain text body",
                "<p>HTML body</p>"
        );

        ArgumentCaptor<MailjetRequest> captor = ArgumentCaptor.forClass(MailjetRequest.class);
        verify(mailjetClient).post(captor.capture());

        JSONObject body = new JSONObject(captor.getValue().getBody());
        JSONArray messages = body.getJSONArray("Messages");
        JSONObject firstMessage = messages.getJSONObject(0);

        assertEquals("Hello", firstMessage.getString("Subject"));
        assertEquals("Plain text body", firstMessage.getString("TextPart"));
        assertEquals("<p>HTML body</p>", firstMessage.getString("HTMLPart"));
        assertEquals("test@example.com", firstMessage.getJSONArray("To").getJSONObject(0).getString("Email"));
        assertEquals("FinSight", firstMessage.getJSONObject("From").getString("Name"));
    }

    @Test
    void sendEmailThrowsWhenMailjetReturnsError() throws Exception {
        when(mailjetClient.post(any(MailjetRequest.class))).thenReturn(mailjetResponse);
        when(mailjetResponse.getStatus()).thenReturn(500);
        when(mailjetResponse.getRawResponseContent()).thenReturn("failure");

        assertThrows(RuntimeException.class, () -> mailjetEmailService.sendEmail(
                "test@example.com",
                "Hello",
                "Plain text body",
                "<p>HTML body</p>"
        ));
    }
}
