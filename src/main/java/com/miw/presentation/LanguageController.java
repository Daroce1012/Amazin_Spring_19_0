package com.miw.presentation;

import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class LanguageController {
    
    @Autowired
    private LocaleResolver localeResolver;
    
    @GetMapping("/changeLanguage")
    public String changeLanguage(
            @RequestParam("lang") String lang,
            HttpServletRequest request, 
            HttpServletResponse response) {
        
        // Establecer el nuevo locale
        Locale locale = new Locale(lang);
        localeResolver.setLocale(request, response, locale);
        
        // Redirigir a la página anterior (Referer)
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            // Redirigir a la URL completa del referer
            return "redirect:" + referer;
        }
        
        // Si no hay referer, redirigir al menú principal
        return "redirect:/private/menu";
    }
}

