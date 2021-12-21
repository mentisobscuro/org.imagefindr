package org.imagefindr;

import java.util.Objects;

public class FoundImage {
    private String URL;
    private String foundOnURL;
    private String thumbnailURL;
    private String title;

    public FoundImage() {
    }

    public FoundImage(String URL, String foundOnURL, String thumbnailURL, String title) {
        this.URL = URL;
        this.foundOnURL = foundOnURL;
        this.thumbnailURL = thumbnailURL;
        this.title = title;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getFoundOnURL() {
        return foundOnURL;
    }

    public void setFoundOnURL(String foundOnURL) {
        this.foundOnURL = foundOnURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FoundImage image = (FoundImage) o;
        return Objects.equals(URL, image.URL)
                && Objects.equals(foundOnURL, image.foundOnURL)
                && Objects.equals(thumbnailURL, image.thumbnailURL)
                && Objects.equals(title, image.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(URL, foundOnURL, thumbnailURL, title);
    }

    @Override
    public String toString() {
        return "FoundImage{" +
                "URL='" + URL + '\'' +
                ", foundOnURL='" + foundOnURL + '\'' +
                ", thumbnailURL='" + thumbnailURL + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
