package org.imagefindr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
public class ReverseSearchController {
    // SerpApi authorization key

    @PostMapping(
            value = "/reverse",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public ArrayList<FoundImage> getReverseSearchResult(@RequestParam("fileURL") String fileURL) {
        ArrayList<FoundImage> images = new ArrayList<>();
        JsonReader reader = null;

        try {
            // Request to API with image URL and auth key
            URL searchAPI = new URL(
                    "https://serpapi.com/search.json?engine=google_reverse_image&image_url=" +
                            URLEncoder.encode(fileURL, StandardCharsets.UTF_8) +
                            "&api_key=" + Config.SERP_API_KEY
            );
            URLConnection request = searchAPI.openConnection();
            request.connect();

            reader = new JsonReader(new InputStreamReader((InputStream) request.getContent(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Reading response JSON with search results
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject rootObj = gson.fromJson(reader, JsonObject.class);

        Type imagesListType = new TypeToken<ArrayList<JsonObject>>(){}.getType();
        List<JsonObject> imagesList = gson.fromJson(rootObj.get("image_results"), imagesListType);
        // For each search result adding a new image to return array
        for (var image: imagesList) {
            // Only for results with image
            if (image.get("thumbnail") != null) {
                images.add(new FoundImage(
                        image.get("thumbnail_destination_url").getAsString(),
                        image.get("link").getAsString(),
                        image.get("thumbnail_destination_url").getAsString(),
                        image.get("title").getAsString())
                );
            }
        }
        return images;
    }
}
