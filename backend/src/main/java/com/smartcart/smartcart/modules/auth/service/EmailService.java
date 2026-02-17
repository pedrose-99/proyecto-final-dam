package com.smartcart.smartcart.modules.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService 
{
    private final JavaMailSender mailSender;
    
    public void sendPasswordResetEmail(String toEmail, String newPassword)
    {
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("SmartCart - Nueva contraseña");
            message.setText(
                "Hola,\n\n" +
                "Has solicitado restablecer tu contraseña.\n\n" +
                "Tu nueva contraseña es: " + newPassword + "\n\n" +
                "Por seguridad, te recomendamos cambiarla después de iniciar sesión.\n\n" +
                "Saludos,\n" +
                "Equipo SmartCart"
            );
            
            mailSender.send(message);
            log.info("Email de reset de contraseña enviado a: {}", toEmail);
        }
        catch (Exception e)
        {
            log.error("Error enviando email a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error al enviar el email");
        }
    }
}
