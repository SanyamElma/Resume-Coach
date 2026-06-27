package com.resumeanalyzer.ai.skill;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical skill taxonomy plus an alias/synonym index, used by the deterministic skill
 * engine. This is the single source of truth that lets us match "JS" ≡ "JavaScript",
 * "ReactJS" ≡ "React", "k8s" ≡ "Kubernetes", etc. WITHOUT relying on the LLM.
 *
 * <p>Each canonical skill has a category (used for ATS weighting and gap grouping) and a
 * set of aliases. {@link #normalize(String)} resolves any surface form to its canonical
 * skill, which is the basis for exact + synonym matching.</p>
 */
@Component
public class SkillDictionary {

    public enum Category { LANGUAGE, FRAMEWORK, DATABASE, CLOUD, DEVOPS, TOOL, CONCEPT, SOFT }

    /** A canonical skill with its category and recognised aliases. */
    public record Skill(String canonical, Category category, Set<String> aliases) {}

    // canonical (lowercased) -> Skill
    private final Map<String, Skill> canonicalIndex = new LinkedHashMap<>();
    // any alias/surface form (lowercased) -> canonical (lowercased)
    private final Map<String, String> aliasIndex = new LinkedHashMap<>();

    public SkillDictionary() {
        // --- Languages ---
        register("Java", Category.LANGUAGE, "core java", "java se", "java8", "java 8", "java11", "java17");
        register("JavaScript", Category.LANGUAGE, "js", "ecmascript", "es6", "vanilla js");
        register("TypeScript", Category.LANGUAGE, "ts");
        register("Python", Category.LANGUAGE, "py", "python3");
        register("SQL", Category.LANGUAGE, "structured query language", "t-sql", "pl/sql");
        register("Go", Category.LANGUAGE, "golang");
        register("C#", Category.LANGUAGE, "csharp", "c sharp", "dotnet c#");
        register("Kotlin", Category.LANGUAGE);
        register("HTML", Category.LANGUAGE, "html5");
        register("CSS", Category.LANGUAGE, "css3");

        // --- Frameworks / libraries ---
        register("Spring Boot", Category.FRAMEWORK, "spring", "springboot", "spring framework", "spring mvc");
        register("Hibernate", Category.FRAMEWORK, "jpa", "spring data jpa");
        register("React", Category.FRAMEWORK, "reactjs", "react.js", "react js");
        register("Node.js", Category.FRAMEWORK, "node", "nodejs", "node js");
        register("Express", Category.FRAMEWORK, "expressjs", "express.js");
        register("Angular", Category.FRAMEWORK, "angularjs", "angular.js");
        register("Vue", Category.FRAMEWORK, "vuejs", "vue.js");
        register("Django", Category.FRAMEWORK);
        register("Flask", Category.FRAMEWORK);
        register("Redux", Category.FRAMEWORK);
        register("Tailwind CSS", Category.FRAMEWORK, "tailwind", "tailwindcss");
        register("JUnit", Category.FRAMEWORK, "junit5", "junit 5");

        // --- Databases ---
        register("PostgreSQL", Category.DATABASE, "postgres", "psql", "pgsql");
        register("MySQL", Category.DATABASE);
        register("MongoDB", Category.DATABASE, "mongo");
        register("Redis", Category.DATABASE);
        register("Elasticsearch", Category.DATABASE, "elastic search", "es");
        register("Oracle", Category.DATABASE, "oracle db");

        // --- Cloud ---
        register("AWS", Category.CLOUD, "amazon web services", "ec2", "s3", "lambda");
        register("Azure", Category.CLOUD, "microsoft azure");
        register("GCP", Category.CLOUD, "google cloud", "google cloud platform");

        // --- DevOps ---
        register("Docker", Category.DEVOPS, "containers", "containerization");
        register("Kubernetes", Category.DEVOPS, "k8s");
        register("Jenkins", Category.DEVOPS);
        register("Terraform", Category.DEVOPS);
        register("CI/CD", Category.DEVOPS, "cicd", "ci cd", "continuous integration", "continuous delivery");
        register("Git", Category.DEVOPS, "github", "gitlab", "version control");

        // --- Concepts / architecture ---
        register("Microservices", Category.CONCEPT, "micro services", "micro-services");
        register("REST API", Category.CONCEPT, "rest", "restful", "restful api", "rest apis", "restful apis");
        register("GraphQL", Category.CONCEPT);
        register("Kafka", Category.CONCEPT, "apache kafka");
        register("RabbitMQ", Category.CONCEPT, "rabbit mq");
        register("System Design", Category.CONCEPT, "systems design");
        register("Agile", Category.CONCEPT, "scrum", "agile/scrum", "kanban");

        // --- Tools ---
        register("Maven", Category.TOOL);
        register("Gradle", Category.TOOL);
        register("Linux", Category.TOOL, "unix");
        register("Mockito", Category.TOOL);

        // --- Soft skills ---
        register("Communication", Category.SOFT);
        register("Leadership", Category.SOFT, "team lead", "tech lead");
        register("Problem Solving", Category.SOFT, "problem-solving");
    }

    private void register(String canonical, Category category, String... aliases) {
        String key = canonical.toLowerCase(Locale.ROOT);
        Set<String> aliasSet = new LinkedHashSet<>(Arrays.asList(aliases));
        canonicalIndex.put(key, new Skill(canonical, category, aliasSet));
        aliasIndex.put(key, key);
        for (String alias : aliases) {
            aliasIndex.put(alias.toLowerCase(Locale.ROOT), key);
        }
    }

    /** Resolves any surface form (alias or canonical) to its canonical skill, if known. */
    public Optional<Skill> normalize(String surfaceForm) {
        if (surfaceForm == null || surfaceForm.isBlank()) {
            return Optional.empty();
        }
        String canonicalKey = aliasIndex.get(surfaceForm.toLowerCase(Locale.ROOT).trim());
        return Optional.ofNullable(canonicalKey).map(canonicalIndex::get);
    }

    /** All known surface forms (canonical + aliases), longest first for greedy text scanning. */
    public List<String> allSurfaceForms() {
        return aliasIndex.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
    }

    public String canonicalOf(String surfaceForm) {
        return normalize(surfaceForm).map(Skill::canonical).orElse(null);
    }

    public Category categoryOf(String canonical) {
        Skill skill = canonicalIndex.get(canonical.toLowerCase(Locale.ROOT));
        return skill == null ? Category.TOOL : skill.category();
    }
}
