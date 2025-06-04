package app.web;

import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import app.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/wallets")
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;

    @Autowired
    public WalletController(UserService userService,
                            WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }


    @GetMapping
    public ModelAndView getWalletsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById (authenticationMetadata.getUserId ());

        ModelAndView modelAndView = new ModelAndView ();
        modelAndView.setViewName ("wallets");
        modelAndView.addObject ("user", user);

        return modelAndView;
    }



    @PostMapping
    public String createNewWallet(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById (authenticationMetadata.getUserId ());

        walletService.unlockNewWallet (user);

        return "redirect:/wallets";
    }
}