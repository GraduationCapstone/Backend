package com.graduationCapstone.Probe.global.init;

import com.graduationCapstone.Probe.domain.project.entity.Guide;
import com.graduationCapstone.Probe.domain.project.repository.GuideRepository;
import com.graduationCapstone.Probe.domain.test.entity.ScenarioSerial;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuideInitializer implements ApplicationRunner {

    private final GuideRepository guideRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (guideRepository.count() > 0) {
            log.info("가이드 데이터가 이미 존재하여 초기화를 건너뜁니다.");
            return;
        }

        log.info("Enum 기반 Guide 마스터 데이터 동기화 시작...");

        for (ScenarioSerial serial : ScenarioSerial.values()) {
            // 이미 존재하면 건너뛰고, 없으면 새로 저장 (Duplicate 방지)
            if (!guideRepository.existsByTestItem(serial.getTestItem())) {
                Guide guide = Guide.builder()
                        .category(serial.getCategory())
                        .testItem(serial.getTestItem())
                        .build();
                guideRepository.save(guide);
            }
        }

        log.info("Guide 마스터 데이터 동기화 완료.");
    }
}