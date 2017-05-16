package org.lib.rpc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
@Controller
public class HelloWorldController {



    @RequestMapping("/helloWorld")
    public String getInfo(){
    System.out.println("----------info-----------");
        return "main";
    }

    @RequestMapping("/info")
    @ResponseBody
    public String getUserInfo(){
        ModelAndView mv = new ModelAndView();
        String kk=null;

        mv.addObject(kk);
        mv.addObject("");
        return "";
    }

    public void getList(){
        List list =new ArrayList();
        Set set = new HashSet();

    }
}
