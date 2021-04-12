package com.nc.tradox.api;

import com.nc.tradox.service.TradoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("api/v1/registration")
@CrossOrigin("*")
public class RegistrationController {
    private final TradoxService tradoxService;

    @Autowired
    public RegistrationController(TradoxService tradoxService) {
        this.tradoxService = tradoxService;
    }

    @PostMapping("/fill")
    public String registration(@RequestBody Map<String, String> json, BindingResult bindingResult, HttpSession httpSession) {
        if (!bindingResult.hasErrors()) {
            return tradoxService.registerUser(json, httpSession);
        }
        return "{\"result\": false, \"emailNotUnique\": false, \"passportNotUnique\": false}";
    }

}
