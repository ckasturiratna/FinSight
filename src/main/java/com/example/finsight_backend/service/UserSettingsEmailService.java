package com.example.finsight_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsEmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordChangeOtp(String email, String firstName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("Password Change Verification - FinSight");
            
            String htmlContent = getPasswordChangeOtpTemplate(firstName, otp);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Password change OTP email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending password change OTP email", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sendEmailChangeOtp(String email, String firstName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("Email Change Verification - FinSight");
            
            String htmlContent = getEmailChangeOtpTemplate(firstName, otp);
            helper.setText(htmlContent, true);
            
        mailSender.send(message);
            log.info("Email change OTP email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending email change OTP email", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sendAccountDeletionOtp(String email, String firstName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("Account Deletion Verification - FinSight");
            
            String htmlContent = getAccountDeletionOtpTemplate(firstName, otp);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Account deletion OTP email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending account deletion OTP email", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    private String getPasswordChangeOtpTemplate(String firstName, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Change Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px solid #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Change Verification</h1>
                        <p>FinSight Security Center</p>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>You have requested to change your password for your FinSight account. To complete this process, please use the verification code below:</p>
                        
                        <div class="otp-box">
                            <p style="margin: 0 0 10px 0; color: #666;">Your verification code:</p>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Important Security Information:</strong>
                            <ul>
                                <li>This code will expire in 10 minutes</li>
                                <li>Never share this code with anyone</li>
                                <li>If you didn't request this change, please contact support immediately</li>
                            </ul>
                        </div>
                        
                        <p>If you didn't request this password change, please ignore this email and consider changing your account password for security.</p>
                        
                        <div class="footer">
                            <p>This is an automated message from FinSight. Please do not reply to this email.</p>
                            <p>&copy; 2024 FinSight. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, otp);
    }

    private String getEmailChangeOtpTemplate(String firstName, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Change Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px solid #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 Email Change Verification</h1>
                        <p>FinSight Security Center</p>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>You have requested to change your email address for your FinSight account. To complete this process, please use the verification code below:</p>
                        
                        <div class="otp-box">
                            <p style="margin: 0 0 10px 0; color: #666;">Your verification code:</p>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Important Security Information:</strong>
                            <ul>
                                <li>This code will expire in 10 minutes</li>
                                <li>Never share this code with anyone</li>
                                <li>If you didn't request this change, please contact support immediately</li>
                            </ul>
                        </div>
                        
                        <p>If you didn't request this email change, please ignore this email and consider changing your account password for security.</p>
                        
                        <div class="footer">
                            <p>This is an automated message from FinSight. Please do not reply to this email.</p>
                            <p>&copy; 2024 FinSight. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, otp);
    }

    private String getAccountDeletionOtpTemplate(String firstName, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Deletion Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px solid #e74c3c; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #e74c3c; letter-spacing: 5px; }
                    .warning { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🗑️ Account Deletion Verification</h1>
                        <p>FinSight Security Center</p>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>You have requested to delete your FinSight account. This action is <strong>IRREVERSIBLE</strong> and will permanently remove all your data.</p>
                        
                        <div class="otp-box">
                            <p style="margin: 0 0 10px 0; color: #666;">Your verification code:</p>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ CRITICAL WARNING:</strong>
                            <ul>
                                <li>This action is <strong>PERMANENT</strong> and cannot be undone</li>
                                <li>All your portfolios, alerts, and data will be permanently deleted</li>
                                <li>This code will expire in 10 minutes</li>
                                <li>If you didn't request this deletion, please contact support immediately</li>
                            </ul>
                        </div>
                        
                        <p><strong>If you're sure you want to proceed</strong>, use the verification code above to confirm the deletion of your account.</p>
                        
                        <div class="footer">
                            <p>This is an automated message from FinSight. Please do not reply to this email.</p>
                            <p>&copy; 2024 FinSight. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, otp);
    }
}