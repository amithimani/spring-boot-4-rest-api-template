package me.dio.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import me.dio.model.Hero;
import me.dio.repository.HeroRepository;

/**
 * End-to-end tests of the Heroes API against an in-memory H2 database.
 * They pin down the current HTTP contract so framework upgrades can be verified.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeroControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HeroRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private Hero saveHero(String name, int xp) {
        Hero hero = new Hero();
        hero.setName(name);
        hero.setXp(xp);
        return repository.save(hero);
    }

    @Test
    void createReturns201WithLocationAndStartsXpAtZero() throws Exception {
        mvc.perform(post("/heroes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Batman\",\"xp\":999}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/heroes/" + repository.findAll().get(0).getId())))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Batman"))
            .andExpect(jsonPath("$.xp").value(0));
    }

    @Test
    void findAllIsSortedByXpDescending() throws Exception {
        saveHero("Low", 1);
        saveHero("High", 10);
        saveHero("Mid", 5);

        mvc.perform(get("/heroes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("High"))
            .andExpect(jsonPath("$[1].name").value("Mid"))
            .andExpect(jsonPath("$[2].name").value("Low"));
    }

    @Test
    void findByIdReturnsHero() throws Exception {
        Hero hero = saveHero("Robin", 3);

        mvc.perform(get("/heroes/{id}", hero.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Robin"))
            .andExpect(jsonPath("$.xp").value(3));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        mvc.perform(get("/heroes/{id}", 12345))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateChangesOnlyName() throws Exception {
        Hero hero = saveHero("Old", 7);

        mvc.perform(put("/heroes/{id}", hero.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + hero.getId() + ",\"name\":\"New\",\"xp\":1000}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.xp").value(7));
    }

    @Test
    void updateWithMismatchedIdReturns422() throws Exception {
        Hero hero = saveHero("Hero", 0);

        mvc.perform(put("/heroes/{id}", hero.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + (hero.getId() + 1) + ",\"name\":\"X\"}"))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        mvc.perform(put("/heroes/{id}", 12345)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":12345,\"name\":\"X\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204ThenHeroIsGone() throws Exception {
        Hero hero = saveHero("Temp", 0);

        mvc.perform(delete("/heroes/{id}", hero.getId()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/heroes/{id}", hero.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        mvc.perform(delete("/heroes/{id}", 12345))
            .andExpect(status().isNotFound());
    }

    @Test
    void upIncreasesXpByTwo() throws Exception {
        Hero hero = saveHero("Flash", 4);

        mvc.perform(patch("/heroes/{id}/up", hero.getId()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/heroes/{id}", hero.getId()))
            .andExpect(jsonPath("$.xp").value(6));
    }

    @Test
    void upReturns404WhenMissing() throws Exception {
        mvc.perform(patch("/heroes/{id}/up", 12345))
            .andExpect(status().isNotFound());
    }

    @Test
    void createIgnoresClientIdAndNeverOverwritesExistingHero() throws Exception {
        Hero existing = saveHero("Original", 50);

        mvc.perform(post("/heroes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + existing.getId() + ",\"name\":\"Impostor\",\"version\":0}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Impostor"))
            .andExpect(jsonPath("$.id").value(not(existing.getId().intValue())));

        mvc.perform(get("/heroes/{id}", existing.getId()))
            .andExpect(jsonPath("$.name").value("Original"))
            .andExpect(jsonPath("$.xp").value(50));
    }

    @Test
    void createWithBlankNameReturns400ProblemDetail() throws Exception {
        mvc.perform(post("/heroes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void notFoundIsReportedAsProblemDetail() throws Exception {
        mvc.perform(get("/heroes/{id}", 12345))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Resource ID 12345 not found."));
    }

    @Test
    void updateWithCurrentVersionSucceedsAndBumpsVersion() throws Exception {
        Hero hero = saveHero("Old", 0);

        mvc.perform(put("/heroes/{id}", hero.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"version\":" + hero.getVersion() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.version").value(hero.getVersion() + 1));
    }

    @Test
    void updateWithStaleVersionReturns409() throws Exception {
        Hero hero = saveHero("Old", 0);
        mvc.perform(patch("/heroes/{id}/up", hero.getId())); // bumps the version

        mvc.perform(put("/heroes/{id}", hero.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"version\":" + hero.getVersion() + "}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mvc.perform(get("/heroes/{id}", hero.getId()))
            .andExpect(jsonPath("$.name").value("Old"));
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/heroes']").exists());
    }
}
