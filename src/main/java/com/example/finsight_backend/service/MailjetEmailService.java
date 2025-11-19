package com.example.finsight_backend.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailjetEmailService {

    private static final String FROM_EMAIL = "cskasturiratna@gmail.com";
    private static final String FROM_NAME = "FinSight";

    private MailjetClient mailjetClient;

    @Autowired
    public MailjetEmailService(@Value("${MAILJET_API_KEY:}") String apiKey,
                               @Value("${MAILJET_SECRET_KEY:}") String secretKey) {
        this(new MailjetClient(apiKey, secretKey));
    }

    private MailjetEmailService(MailjetClient mailjetClient) {
        this.mailjetClient = mailjetClient;
    }

    void setMailjetClient(MailjetClient mailjetClient) {
        this.mailjetClient = mailjetClient;
    }

    public void sendEmail(String to, String subject, String textBody, String htmlBody) {
        try {
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", FROM_EMAIL)
                                            .put("Name", FROM_NAME))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject().put("Email", to)))
                                    .put(Emailv31.Message.SUBJECT, subject)
                                    .put(Emailv31.Message.TEXTPART, textBody)
                                    .put(Emailv31.Message.HTMLPART, htmlBody)
                            ));

            MailjetResponse response = mailjetClient.post(request);
            int status = response.getStatus();
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Failed to send email via Mailjet. Status: " + status +
                        ", response: " + response.getRawResponseContent());
            }
            log.info("Mailjet email sent to {} with subject '{}', status {}", to, subject, status);
        } catch (JSONException e) {
            throw new RuntimeException("Failed to build Mailjet request payload", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Mailjet", e);
        }
    }
}
