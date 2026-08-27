package com.zipdaprojecttak.global.id;

import com.github.f4b6a3.tsid.TsidCreator;
import org.springframework.stereotype.Component;

@Component
public class TsidGenerator {

    public Long generate() {
        return TsidCreator
                .getTsid()
                .toLong();
    }
}