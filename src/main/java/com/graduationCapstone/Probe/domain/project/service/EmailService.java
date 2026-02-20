package com.graduationCapstone.Probe.domain.project.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * 비동기로 프로젝트 초대 이메일을 발송합니다.
     * @Async("mailExecutor")를 통해 별도의 스레드에서 실행됩니다.
     */
    @Async("mailExecutor")
    public void sendInvitationEmail(String toEmail, String inviteLink, String projectName) {
        log.info("이메일 발송 시작 : to={}, project={}", toEmail, projectName);

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);

            helper.setSubject(String.format("[Probe] '%s' 프로젝트 초대장이 도착했습니다!", projectName));

            Context context = new Context();
            context.setVariable("serviceName", "Probe");
            context.setVariable("projectName", projectName);
            context.setVariable("inviteLink", inviteLink);

            String htmlContent = templateEngine.process("mail/invitation", context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("이메일 발송 성공: to={}", toEmail);

        } catch (MessagingException e) {
            log.error("이메일 발송 중 오류 발생: to={}, error={}", toEmail, e.getMessage());
        }
    }
}
