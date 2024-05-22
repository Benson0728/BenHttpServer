package com.benson.controller;

import com.alibaba.fastjson2.JSONObject;
import com.benson.annotation.*;
import com.benson.vo.Result;

@Controller
public class TestController2 {
    @Get("/test4")
    public Result testGet4(@RequestParam("name") String name){
        return new Result(1,name,1);
    }

    @Post("/test5")
    public Result testPost5(@RequestBody JSONObject jsonObject){
        String name = String.valueOf(jsonObject.get("name"));
        return new Result(1,name,jsonObject);
    }
    @Post("/test6")
    public Result testPost6( String name, @RequestHeader("nihao") Long nihao){
        return new Result(1,name,nihao);
    }
    @Post("/test7/{num}")
    public Result testPost7(@RequestParam("fuck") String fuck, @PathVariable("num")Long num){
        return new Result(1,fuck,num);
    }
    @Put("/test8/{id}")
    public Result testPut8(@PathVariable("id") Long id,@RequestParam("name") String name){
        return new Result(1,name,id);
    }

    @Delete("/test9/{id}")
    public Result testDelete(@PathVariable("id") Long id){
        return new Result(200,"删除"+id+"成功",null);
    }
}
