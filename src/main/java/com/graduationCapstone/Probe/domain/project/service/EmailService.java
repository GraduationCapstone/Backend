package com.graduationCapstone.Probe.domain.project.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendInvitationEmail(String toEmail, String inviteLink, String projectName) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);

            helper.setSubject(String.format("[Probe] '%s' 프로젝트 초대장이 도착했습니다!", projectName));

            String htmlContent = getInvitationHtml(inviteLink, projectName);

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private String getInvitationHtml(String inviteLink, String projectName) {
        return """
        <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
            <h2 style="color: #333;">프로젝트 초대장</h2>
            <p style="font-size: 16px; color: #555;">
                안녕하세요! <strong>Probe</strong> 서비스입니다.<br>
                아래 프로젝트로부터 초대장이 도착했습니다.
            </p>
            <div style="background-color: #f8f9fa; padding: 15px; border-left: 4px solid #4CAF50; margin: 20px 0;">
                <span style="font-size: 18px; font-weight: bold; color: #2e7d32;">
                    📂 프로젝트명: %s
                </span>
            </div>
            <p style="font-size: 14px; color: #777; margin-bottom: 30px;">
                팀원들과 함께 협업을 시작하려면 아래 '초대 수락하기' 버튼을 클릭해 주세요.
            </p>
            <div style="text-align: center;">
                <a href="%s" style="background-color: #4CAF50; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                    초대 수락하기
                </a>
            </div>
        </div>
        """.formatted(projectName, inviteLink);
    }
}
