package com.benson.controller;

import com.benson.annotation.*;
import com.benson.vo.Result;

@Controller
public class TestController1 {
    @Get("/test1/{number}")
    public Result testGet1(@PathVariable("number") Long number, @RequestParam("name") String name ){
        return new Result(1,name,number);
    }

    @Get("/test2")
    public Result testGet2(@RequestHeader("fuck") String name){
        return new Result(1,name,1);
    }
    @Get("/test3")
    public Result testGet3(String name){
        return new Result(1,name,1);
    }
}
