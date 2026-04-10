package com.eventitta.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import software.amazon.awssdk.services.s3.S3Client;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.eventitta", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularStructureArchTest {

    @ArchTest
    static final ArchRule legacyPackageRootsMustBeGone =
        noClasses()
            .should()
            .resideInAnyPackage(
                "com.eventitta.bootstrap..",
                "com.eventitta.platform..",
                "com.eventitta.shared..",
                "com.eventitta.common.."
            );

    @ArchTest
    static final ArchRule noClassesOutsideAppMayDependOnApp =
        noClasses()
            .that()
            .resideOutsideOfPackage("com.eventitta.app..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.eventitta.app..");

    @ArchTest
    static final ArchRule domainMustNotDependOnApiInfraOrApp =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.eventitta.api..",
                "com.eventitta.infra..",
                "com.eventitta.app.."
            );

    @ArchTest
    static final ArchRule apiMustNotDependOnInfra =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.eventitta.infra..");

    @ArchTest
    static final ArchRule apiMustNotDependOnDomainEntities =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.api..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule apiAuthMustNotContainBusinessServices =
        noClasses()
            .should()
            .resideInAnyPackage("com.eventitta.api.auth.service..");

    @ArchTest
    static final ArchRule infraMustNotDependOnApi =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.infra..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.eventitta.api..");

    @ArchTest
    static final ArchRule authMustUseUserInternalApiInsteadOfUserPersistenceOrEntity =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain.auth..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.eventitta.domain.user.domain..",
                "com.eventitta.domain.user.repository.."
            );

    @ArchTest
    static final ArchRule mediaMustUseUserInternalApiInsteadOfUserPersistenceOrEntity =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain.media..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.eventitta.domain.user.domain..",
                "com.eventitta.domain.user.repository.."
            );

    @ArchTest
    static final ArchRule nonFileDomainsMustNotDependOnFileServiceImplementations =
        noClasses()
            .that()
            .resideOutsideOfPackages(
                "com.eventitta.domain.file..",
                "com.eventitta.domain.common..",
                "com.eventitta.infra..",
                "com.eventitta.api..",
                "com.eventitta.app.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.eventitta.domain.file.service..");

    @ArchTest
    static final ArchRule fileControllerMustUseMediaInternalApiInsteadOfMediaServices =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.api.file.controller..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.eventitta.domain.media.service..",
                "com.eventitta.domain.media.dto.."
            );

    @ArchTest
    static final ArchRule domainMustNotUseJpaRepository =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(JpaRepository.class);

    @ArchTest
    static final ArchRule domainMustNotUseJdbcTemplateLikeClients =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(JdbcTemplate.class);

    @ArchTest
    static final ArchRule domainMustNotUseRedisTemplate =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(RedisTemplate.class);

    @ArchTest
    static final ArchRule domainMustNotUseRestClient =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(RestClient.class);

    @ArchTest
    static final ArchRule domainMustNotUseS3Client =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(S3Client.class);

    @ArchTest
    static final ArchRule domainMustNotUseMultipartFile =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(MultipartFile.class);

    @ArchTest
    static final ArchRule domainMustNotUseResource =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Resource.class);

    @ArchTest
    static final ArchRule domainMustNotUseMediaType =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(MediaType.class);

    @ArchTest
    static final ArchRule domainMustNotUseConfigurationProperties =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(ConfigurationProperties.class.getName());

    @ArchTest
    static final ArchRule domainMustNotUseGetExchange =
        noClasses()
            .that()
            .resideInAnyPackage("com.eventitta.domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(GetExchange.class.getName());

    @ArchTest
    static final ArchRule keyBoundedContextsMustUseInternalApisAcrossDomains =
        classes()
            .that()
            .resideInAnyPackage(
                "com.eventitta.domain.region..",
                "com.eventitta.domain.post..",
                "com.eventitta.domain.comment..",
                "com.eventitta.domain.meeting..",
                "com.eventitta.domain.gamification.."
            )
            .should(avoidForeignRepositoriesServicesAndEntities());

    private static ArchCondition<JavaClass> avoidForeignRepositoriesServicesAndEntities() {
        return new ArchCondition<>("use foreign domains only through api.internal contracts") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String sourceDomain = extractDomain(item.getPackageName());
                if (sourceDomain == null) {
                    return;
                }

                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetDomain = extractDomain(target.getPackageName());
                    if (targetDomain == null
                        || targetDomain.equals(sourceDomain)
                        || targetDomain.equals("common")
                    ) {
                        continue;
                    }
                    if (target.getPackageName().contains(".api.internal.")) {
                        continue;
                    }
                    if (!isForeignRepositoryServiceOrEntity(target)) {
                        continue;
                    }

                    String message = item.getFullName()
                        + " depends on foreign type "
                        + target.getFullName()
                        + " without going through api.internal";
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }

    private static boolean isForeignRepositoryServiceOrEntity(JavaClass target) {
        String packageName = target.getPackageName();
        return packageName.contains(".repository.")
            || packageName.contains(".service.")
            || target.isAnnotatedWith(Entity.class);
    }

    private static String extractDomain(String packageName) {
        String prefix = "com.eventitta.domain.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }

        String remainder = packageName.substring(prefix.length());
        int dotIndex = remainder.indexOf('.');
        if (dotIndex < 0) {
            return remainder;
        }
        return remainder.substring(0, dotIndex);
    }
}
