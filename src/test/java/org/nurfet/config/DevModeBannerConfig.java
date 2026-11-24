package org.nurfet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;

@TestConfiguration(proxyBeanMethods = false)
@Slf4j
public class DevModeBannerConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║  DEV MODE: Запущен временный PostgreSQL-контейнер          ║");
        log.warn("║  Все данные будут УНИЧТОЖЕНЫ при остановке приложения!     ║");
        log.warn("║  Не используйте этот режим в production!                   ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
    }
}
