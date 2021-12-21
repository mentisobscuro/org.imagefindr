package org.imagefindr;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
public class SearchController {
    // SerpApi authorization key

    @PostMapping(
            value = "/search",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public ArrayList<FoundImage> getSearchResult(@RequestParam("searchPrompt") String searchPrompt) {
        ArrayList<FoundImage> images = new ArrayList<>();
        JsonReader reader = null;

        try {
            // Request to API with search prompt and auth key
            URL searchAPI = new URL(
                    "https://serpapi.com/search.json?q=" +
                            URLEncoder.encode(searchPrompt, StandardCharsets.UTF_8) +
                            "&tbm=isch&ijn=0&api_key=" + Config.SERP_API_KEY
            );
            URLConnection request = searchAPI.openConnection();
            request.connect();

            reader = new JsonReader(new InputStreamReader((InputStream) request.getContent(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Reading response JSON with search results
        Gson gson = new Gson();
        JsonObject rootObj = gson.fromJson(reader, JsonObject.class);

        Type imagesListType = new TypeToken<ArrayList<JsonObject>>(){}.getType();
        List<JsonObject> imagesList = gson.fromJson(rootObj.get("images_results"), imagesListType);
        // For each search result adding a new image to return array

        for (var image: imagesList) {
            if (image.get("original") != null && image.get("link") != null && image.get("thumbnail") != null && image.get("title") != null) {
                images.add(new FoundImage(
                        image.get("original").getAsString(),
                        image.get("link").getAsString(),
                        image.get("thumbnail").getAsString(),
                        image.get("title").getAsString())
                );
            }
        }
        return images;
    }
}
