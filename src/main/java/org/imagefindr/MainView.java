package org.imagefindr;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


@Route("/")
@PageTitle("ImageFindr")
public class MainView extends VerticalLayout implements PageConfigurator, RouterLayout {

    private static final float HEADER_HEIGHT = 120.0f;
    private static final String HEADER_BG_COLOR = "#3e6073";
    private static final float HEADER_HEADING_FONT_SIZE = 60.0f;
    private static final float LOGO_OFFSET = 15.0f;
    private static final float IMAGE_MAX_WIDTH = 600.0f;
    private static final float IMAGE_MAX_HEIGHT = 250.0f;
    private static final float BOARDER_RADIUS = 10.0f;

    private final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

    private VerticalLayout resultBlock;

    public MainView() {
        add(
                getHeader(),
                getSearchBlock(),
                getResultBlock()
        );
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addFavIcon("icon", "static/favicon.ico", "192x192");
    }

    private Header getHeader() {
        // Creating website logo
        Image logo = new Image("static/logo.png", "Logo");
        logo.setMaxHeight(HEADER_HEIGHT - 2 * LOGO_OFFSET + "px");
        logo.getStyle().set("margin-left", LOGO_OFFSET + "px");
        logo.getStyle().set("margin-top", LOGO_OFFSET + "px");
        // Creating website heading
        H1 heading = new H1("ImageFindr");
        heading.getStyle().set("color", "#fcfeff");
        heading.getStyle().set("vertical-align", "top");
        heading.getStyle().set("font-size", HEADER_HEADING_FONT_SIZE + "px");
        heading.getStyle().set("display", "inline-block");
        heading.getStyle().set("margin-top", (HEADER_HEIGHT - HEADER_HEADING_FONT_SIZE) / 2.0f + "px");
        heading.getStyle().set("margin-left", LOGO_OFFSET + "px");
        // Creating header block
        Header header = new Header(new Anchor("/", logo, heading));
        header.getStyle().set("background-color", HEADER_BG_COLOR);
        header.getStyle().set("display", "block");
        header.getStyle().set("border-radius", BOARDER_RADIUS + "px");
        header.getStyle().set("width", "100%");
        header.getStyle().set("height", HEADER_HEIGHT+"px");

        return header;
    }

    private HorizontalLayout getSearchBlock() {
        // Creating main objects: search field, button, file upload field, and a block to contain them
        TextField searchField = new TextField();
        Button searchButton = new Button("Search");
        Upload fileUpload = new Upload(new MemoryBuffer());
        HorizontalLayout searchBlock = new HorizontalLayout(searchField, searchButton, fileUpload);

        searchField.setWidth("40%");
        // Trimming useless spaces
        searchField.addValueChangeListener(event ->
                searchField.setValue(searchField.getValue().trim())
        );
        // Adding listener to button click to initiate search
        searchButton.addClickListener(click -> {
            String searchPrompt = searchField.getValue();
            if (!searchPrompt.equals("")) {
                fileUpload.getElement().executeJs("this.files=[]");
                // Request to server with search prompt parameter to get the array of images
                ArrayList<FoundImage> images = searchAndGetResultImages(searchPrompt);
                // Displaying found images on the webpage
                displayResultImages(images);
            }
            else {
                // If no input present - display an error dialog
                showErrorDialog("Input something!");
            }
        });
        searchButton.addClickShortcut(Key.ENTER);

        fileUpload.getStyle().set("display", "inline-block");
        fileUpload.getStyle().set("border", "0");
        // Setting upload to only accept images
        fileUpload.setAcceptedFileTypes(
                "image/jpg", "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg"
        );
        // If file uploaded successfully
        fileUpload.addSucceededListener(event -> {
            searchField.clear();
            // Initializing the array of images with server's response or an empty array if response is empty
            ArrayList<FoundImage> images = uploadFileAndGetResultImages(event);
            // Displaying found images of the webpage
            displayResultImages(images);
        });
        // If file type is wrong
        fileUpload.addFileRejectedListener(event -> {
            // If file of wrong type uploaded - display an error dialog
            showErrorDialog("Wrong file type! Upload images only!");
        });
        // Setting search block style and aligning its content
        searchBlock.setWidth("100%");
        searchBlock.getStyle().set("border-radius", BOARDER_RADIUS + "px");
        searchBlock.getStyle().set("border", "2px solid " + HEADER_BG_COLOR);
        searchBlock.setAlignItems(Alignment.CENTER);
        searchBlock.setJustifyContentMode(JustifyContentMode.CENTER);

        return searchBlock;
    }

    private void displayResultImages(ArrayList<FoundImage> images) {
        // Clearing old results
        getResultBlock().removeAll();
        if (images != null) {
            if (images.size() > 0) {
                // Displaying images one by one
                for (FoundImage image : images) {
                    getResultBlock().add(getResultTemplate(image));
                }
            }
            else {
                // If no similar images found - display error dialog
                showErrorDialog("No similar images found.");
            }
        }
    }

    private VerticalLayout getResultBlock() {
        // Initializing result block if not initialized
        if (resultBlock == null) {
            resultBlock = new VerticalLayout();
            resultBlock.setPadding(false);
        }
        return resultBlock;
    }

    private ArrayList<FoundImage> searchAndGetResultImages(String searchPrompt) {
        // Creating POST request to /search controller
        WebClient client = WebClient.create(baseUrl);
        WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec = client.method(HttpMethod.POST);
        WebClient.RequestBodySpec bodySpec = uriSpec.uri("/search");
        // Adding search prompt value as a parameter
        LinkedMultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();
        valueMap.add("searchPrompt", searchPrompt);
        // Sending request and returning list of images
        return postRequestAngRetrieveImagesList(bodySpec, valueMap);
    }

    private ArrayList<FoundImage> postRequestAngRetrieveImagesList(
            WebClient.RequestBodySpec bodySpec, LinkedMultiValueMap<String, Object> valueMap
    ) {
        // Setting request headers
        WebClient.RequestHeadersSpec<?> headersSpec = bodySpec.body(
                BodyInserters.fromMultipartData(valueMap));
        ParameterizedTypeReference<ArrayList<FoundImage>> imagesListType = new ParameterizedTypeReference<>() {};
        // Setting content type, content to accept, and charset
        WebClient.ResponseSpec responseSpec = headersSpec.header(
                HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve();
        // Retrieving and returning the list of images
        return responseSpec.bodyToMono(imagesListType).block();
    }

    private ArrayList<FoundImage> uploadFileAndGetResultImages(SucceededEvent event) {
        // Saving file to temp directory
        File uploadedFile = new File("src/main/webapp/temp/" + event.getFileName());
        MemoryBuffer buffer = (MemoryBuffer) event.getSource().getReceiver();
        try {
            FileUtils.copyInputStreamToFile(buffer.getInputStream(), uploadedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Creating POST request to /reverse controller
        WebClient client = WebClient.create(baseUrl);
        WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec = client.method(HttpMethod.POST);
        WebClient.RequestBodySpec bodySpec = uriSpec.uri("/reverse");
        // Uploading the image to the cloud and retrieving its URL
        String fileURL = uploadImageToCloudinaryAndGetURL(uploadedFile);
        // Deleting the image from temp directory
        uploadedFile.delete();
        // Adding image URL as a parameter
        LinkedMultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();
        valueMap.add("fileURL", fileURL);
        // Sending request and returning list of images
        return postRequestAngRetrieveImagesList(bodySpec, valueMap);
    }

    private HorizontalLayout getResultTemplate(FoundImage foundImage) {
        // Creating image, setting its style and adding a link to image to open i
        Image image = new Image(foundImage.getThumbnailURL(), foundImage.getFoundOnURL());
        image.setMaxWidth(IMAGE_MAX_WIDTH + "px");
        image.setMaxHeight(IMAGE_MAX_HEIGHT + "px");
        image.getStyle().set("display", "inline-block");
        image.getStyle().set("cursor", "pointer");
        image.getElement().addEventListener("click", click -> {
            UI.getCurrent().getPage().executeJs("window.open(\"" + foundImage.getURL()+"\", \"_blank\");");
        });
        // Creating a container for the image to center it inside
        HorizontalLayout imageContainer = new HorizontalLayout(image);
        imageContainer.setWidth(IMAGE_MAX_WIDTH + "px");
        imageContainer.getStyle().set("display", "inline-block");
        imageContainer.getStyle().set("text-align", "center");
        imageContainer.getStyle().set("margin", BOARDER_RADIUS + "px");
        imageContainer.getStyle().set("margin-left", BOARDER_RADIUS + 15.0f + "px");
        // Creating a title for the image
        H4 title = new H4(foundImage.getTitle());
        title.getElement().getStyle().set("display", "block");
        title.getElement().getStyle().set("margin", "0");
        // Creating a link to the source of the image
        Anchor foundOnLink = new Anchor(foundImage.getFoundOnURL(), "Source");
        foundOnLink.setTarget("_blank");
        // Creating a link to download the image
        Anchor downloadLink = new Anchor();
        downloadLink.setText("Download");
        downloadLink.getStyle().set("cursor", "pointer");
        downloadLink.getElement().setAttribute("download", "");
        // Downloading the image for user to download from local server
        downloadLink.getElement().addEventListener("click", click -> {
            if (downloadLink.getHref().equals("")) {
                try {
                    File file = new File("src/main/webapp/temp/" + FilenameUtils.getName(foundImage.getURL()));
                    FileUtils.copyURLToFile(new URL(foundImage.getURL()), file);
                    downloadLink.setHref("/temp/" + file.getName());
                    downloadLink.getElement().executeJs("this.click();");
                    file.deleteOnExit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // Creating a container for the title and the links
        Div textContainer = new Div(title, foundOnLink, new Text(" • "), downloadLink);
        textContainer.getStyle().set("display", "inline-block");
        // Creating a container for all the content above
        HorizontalLayout result = new HorizontalLayout(imageContainer, textContainer);
        result.setWidth("100%");
        result.getStyle().set("border-radius", BOARDER_RADIUS + "px");
        result.getStyle().set("border", "2px solid " + HEADER_BG_COLOR);
        result.getStyle().set("padding", "0");
        result.getStyle().set("background-color", "#ffffff");
        result.setAlignItems(Alignment.CENTER);
        result.setJustifyContentMode(JustifyContentMode.START);

        return result;
    }

    private void showErrorDialog(String message) {
        // Creating the dialog block
        Dialog dialog = new Dialog();
        Div content = new Div();
        // Setting text color and text align
        content.getStyle().set("color", "red");
        content.getStyle().set("text-align", "center");
        // Setting the message
        content.setText(message);
        dialog.add(content);
        // Setting dialog width and height
        dialog.setWidth("60%");
        dialog.setHeight("5em");
        // Displaying the dialog
        dialog.open();
    }

    private String uploadImageToCloudinaryAndGetURL(File image) {
        // Cloudinary configuration
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "imagefindr",
                "api_key", Config.CLOUDINARY_API_KEY,
                "api_secret", Config.CLOUDINARY_API_SECRET)
        );
        // Uploading the image and getting its URL
        String url = "";
        try {
            url = cloudinary.uploader().upload(image, ObjectUtils.asMap()).get("url").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }
}