package com.chemexpress.crm.service;

import com.chemexpress.crm.entity.AppMeta;
import com.chemexpress.crm.mapper.AppMetaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AppMetaService {

    private static final String DATA_VERSION_KEY = "data_version";

    private final AppMetaMapper appMetaMapper;
    private final AtomicLong version = new AtomicLong(0);

    public long getDataVersion() {
        long current = version.get();
        if (current > 0) {
            return current;
        }
        synchronized (this) {
            current = version.get();
            if (current > 0) {
                return current;
            }
            AppMeta meta = appMetaMapper.selectById(DATA_VERSION_KEY);
            if (meta == null) {
                meta = new AppMeta();
                meta.setMetaKey(DATA_VERSION_KEY);
                meta.setMetaValue("1");
                appMetaMapper.insert(meta);
            }
            version.set(Long.parseLong(meta.getMetaValue()));
            return version.get();
        }
    }

    public long bumpDataVersion() {
        synchronized (this) {
            long next = getDataVersion() + 1;
            AppMeta meta = new AppMeta();
            meta.setMetaKey(DATA_VERSION_KEY);
            meta.setMetaValue(String.valueOf(next));
            appMetaMapper.updateById(meta);
            version.set(next);
            return next;
        }
    }
}
