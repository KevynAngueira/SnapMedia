// This should be the very first line in images.js (no stray text before the script):
const API_BASE_URL = 'http://10.0.2.2:5000'

// Function to fetch and display images from the first API
function loadImages() {
    fetch('/images/list')
        .then(response => response.json())
        .then(data => {
            const images = data.images || [];
            const imageListContainer = document.getElementById('image-list');

            if (images.length > 0) {
                let imageListHtml = '<ul>';
                images.forEach(image => {
                    // Display the image with its filename and dimensions
                    imageListHtml += `<li>
                                        <img src="/images?${image.contentUri}" alt="${image.name}" width="150" height="150">
                                        <br><strong>${image.name}</strong>
                                        <br>Dimensions: ${image.width}x${image.height}
                                        </li>`;
                });
                imageListHtml += '</ul>';
                imageListContainer.innerHTML = imageListHtml;
            } else {
                imageListContainer.innerHTML = '<p>No images found.</p>';
            }
        })
        .catch(error => {
            console.error('Error fetching images:', error);
        });
}

// Function to send selected images to the second API
function sendImages() {
    const selectedImages = [];
    const images = document.querySelectorAll('#image-list img');

    // Iterate over the images and get their actual filenames (alt attribute) and content URIs
    images.forEach(img => {
        const image = {
            filename: img.alt,  // filename from 'alt' attribute
            uri: img.src.split('?')[1]  // URI from the 'src' attribute (after '?')
        };
        selectedImages.push(image);
    });

    selectedImages.forEach(image => {
        console.log("Sending image:", image.filename);  // Log the filename

        // Fetch the image stream from the Phone API using the content URI
        fetch(`/images?${image.uri}`)
            .then(response => response.blob()) // Convert the image stream to a Blob
            .then(imageBlob => {
                // Create a FormData object to send the image as binary data
                const formData = new FormData();
                formData.append('image', imageBlob, image.filename); // Use the filename as the file name

                // Send the image as FormData to the second API (outside the emulator)
                const request = `${API_BASE_URL}/image/send`;
                fetch(request, {
                    method: 'POST',
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    console.log('Image sent successfully:', data);
                })
                .catch(error => {
                    console.error('Error sending image:', error);
                });
            })
            .catch(error => {
                console.error('Error fetching image stream:', error);
            });
    });
}

function testConnection() {
    const request = `${API_BASE_URL}/test`;
    fetch(request)
        .then(response => {
            console.log('Response status:', response.status);  // Log status
            return response.json();
        })
        .then(data => {
            console.log('Test response:', data);
            alert('Connection to second API is working!');
        })
        .catch(error => {
            console.error('Error testing connection:', error);
            alert('Error connecting to second API.');
        });
}

// Load images when the page is loaded
window.onload = loadImages;