var cellSize = {x: 128, y: 128};
var cells = [];
var lastCell;

var modIdElement;
var nameElement;
var sizeElement;
var markerElement;
var imageElement;
var sourceImageElement;
var debugElement;

function initialize() {
    var sheet = document.getElementById("sheet");

    var cellDimensions = {
        width: Math.ceil(sheet.width / cellSize.x),
        height: Math.ceil(sheet.height / cellSize.y)
    };

    //Preallocate Cells for speed
    cells = [];
    for (var y = 0; y <= cellDimensions.height; ++y) {
        cells[y] = [];
        for (var x = 0; x <= cellDimensions.width; ++x) {
            cells[y][x] = []
        }
    }

    //Add sprite indexes to all the cells they occupy
    var arrayLength = imageData.length;
    for (var i = 0; i < arrayLength; i++) {
        var sprite = imageData[i];

        var cellMinY = Math.floor(sprite.y / cellSize.y);
        var cellMaxY = Math.ceil((sprite.y + sprite.height) / cellSize.y);
        var cellMinX = Math.floor(sprite.x / cellSize.x);
        var cellMaxX = Math.ceil((sprite.x + sprite.width) / cellSize.x);
        for (var y = cellMinY; y <= cellMaxY; ++y) {
            for (var x = cellMinX; x <= cellMaxX; ++x) {
                cells[y][x].push(i);
            }
        }
    }

    //Pre-resolve elements that will be updated
    modIdElement = document.getElementById("modId");
    nameElement = document.getElementById("name");
    sizeElement = document.getElementById("size");
    markerElement = document.getElementById("marker");
    imageElement = document.getElementById("singleImage");
    debugElement = document.getElementById("debug");
    sourceImageElement = document.getElementById("sheet");

    document.getElementById("loading").style.display = "none";

    sheet.addEventListener("mousemove", onMouseMove);
}

function onMouseMove(eventInfo) {
    fastdom.measure(function() {
        eventInfo = eventInfo || window.event;

        var target = eventInfo.target || eventInfo.srcElement;
        var rect = target.getBoundingClientRect();
        var offsetX = eventInfo.clientX - rect.left;
        var offsetY = eventInfo.clientY - rect.top;

        var cellX = Math.floor(offsetX / cellSize.x);
        var cellY = Math.floor(offsetY / cellSize.y);

        if (cells[cellY] !== undefined && cells[cellY][cellX] !== undefined) {
            var containedSprites = cells[cellY][cellX];
            var imageDataIndex = -1;
            for (var i = 0; i < containedSprites.length; ++i) {
                var data = imageData[containedSprites[i]];
                if (offsetX >= data.x && offsetX <= data.x + data.width) {
                    if (offsetY >= data.y && offsetY <= data.y + data.height) {
                        imageDataIndex = containedSprites[i];
                        break;
                    }
                }
            }

            if (imageDataIndex < 0) {
                var imageElementWidth = imageElement.width;
                var imageElementHeight = imageElement.height;
                var ctx = imageElement.getContext("2d");
                ctx.clearRect(0, 0, imageElementWidth, imageElementHeight);
                ctx.globalOpacity = 0.2;
                ctx.fillStyle = "rgba(119,119,119,1)";
                ctx.fillRect(0, 0, imageElementWidth, imageElementHeight);

                fastdom.mutate(function() {
                    nameElement.innerText = "";
                    sizeElement.innerText = "";
                    markerElement.style.display = "none";
                });

                lastCell = -1;
                return;
            }

            if (imageDataIndex != lastCell) {
                lastCell = imageDataIndex;
                var data = imageData[imageDataIndex];

                var targetTop = eventInfo.target.offsetTop;
                var targetLeft = eventInfo.target.offsetLeft;

                var imageElementWidth = imageElement.width;
                var imageElementHeight = imageElement.height;

                var ctx = imageElement.getContext("2d");
                ctx.mozImageSmoothingEnabled = false;
                ctx.msImageSmoothingEnabled = false;
                ctx.imageSmoothingEnabled = false;

                ctx.clearRect(0, 0, imageElementWidth, imageElementHeight);
                ctx.globalOpacity = 0.2;
                ctx.fillStyle = "rgba(119,119,119,1)";
                ctx.fillRect(0, 0, imageElementWidth, imageElementHeight);
                ctx.globalOpacity = 1;
                ctx.drawImage(sourceImageElement,
                    data.x, data.y, data.width, data.height,
                    0, 0, imageElementWidth, imageElementHeight
                );

                fastdom.mutate(function() {
                    modIdElement.innerText = data.name.substring(0, data.name.indexOf(":"));
                    nameElement.innerText = data.name.substring(data.name.indexOf(":") + 1);
                    sizeElement.innerText = data.width + " x " + data.height;

                    markerElement.style.top = data.y + targetTop;
                    markerElement.style.left = data.x + targetLeft;
                    markerElement.style.width = data.width-2;
                    markerElement.style.height = data.height-2;
                    markerElement.style.display = "block";
                });
            }
        } else {
            var imageElementWidth = imageElement.width;
            var imageElementHeight = imageElement.height;
            var ctx = imageElement.getContext("2d");
            ctx.clearRect(0, 0, imageElementWidth, imageElementHeight);
            ctx.globalOpacity = 0.2;
            ctx.fillStyle = "rgba(119,119,119,1)";
            ctx.fillRect(0, 0, imageElementWidth, imageElementHeight);

            fastdom.mutate(function() {
                nameElement.innerText = "";
                sizeElement.innerText = "";
                markerElement.style.display = "none";
            });
            lastCell = -1;
        }
    });
}

function onTextureLoaded() {
    initialize();

    var backgroundImage;
    var atlasBackgroundImage;
    var headerBackgroundImage;

    for (var i = 0; i < imageData.length; i++) {
        if (imageData[i].name == "minecraft:blocks/dirt") {
            backgroundImage = imageData[i];
        }
        if (imageData[i].name == "minecraft:blocks/sandstone_smooth") {
            atlasBackgroundImage = imageData[i];
        }
        if (imageData[i].name == "minecraft:blocks/grass_side") {
            headerBackgroundImage = imageData[i];
        }
    }

    if (backgroundImage != null) {
        try {
            var sourceImageElement = document.getElementById("sheet");
            var dataUrl;

            //var bgCanvas = document.getElementById("backgroundRenderer");
            var bgCanvas = document.createElement("canvas");
            bgCanvas.width = backgroundImage.width * 2;
            bgCanvas.height = backgroundImage.height * 2;

            var ctx = bgCanvas.getContext("2d");
            ctx.mozImageSmoothingEnabled = false;
            ctx.msImageSmoothingEnabled = false;
            ctx.imageSmoothingEnabled = false;

            //Create dark dirt for main page background
            ctx.globalAlpha = 1;
            ctx.drawImage(sourceImageElement,
                backgroundImage.x, backgroundImage.y, backgroundImage.width, backgroundImage.height,
                0, 0, bgCanvas.width, bgCanvas.height
            );
            ctx.globalAlpha = 0.8;
            ctx.fillRect(0, 0, bgCanvas.width, bgCanvas.height);
            dataUrl = bgCanvas.toDataURL();
            document.getElementsByTagName("body")[0].style.backgroundImage = "url(" + dataUrl + ")";

            //Create lighter dirt for header
            ctx.globalAlpha = 1;
            ctx.drawImage(sourceImageElement,
                backgroundImage.x, backgroundImage.y, backgroundImage.width, backgroundImage.height,
                0, 0, bgCanvas.width, bgCanvas.height
            );
            ctx.globalAlpha = 0.5;
            ctx.fillRect(0, 0, bgCanvas.width, bgCanvas.height);
            dataUrl = bgCanvas.toDataURL();
            document.getElementById("header").style.backgroundImage = "url(" + dataUrl + ")";

            //Create sandstone for atlas background
            ctx.globalAlpha = 1;
            ctx.drawImage(sourceImageElement,
                atlasBackgroundImage.x, atlasBackgroundImage.y, atlasBackgroundImage.width, atlasBackgroundImage.height,
                0, 0, bgCanvas.width, bgCanvas.height
            );
            ctx.globalAlpha = 0.8;
            ctx.fillStyle = "white";
            ctx.fillRect(0, 0, bgCanvas.width, bgCanvas.height);
            dataUrl = bgCanvas.toDataURL();
            document.getElementById("sheetContainer").style.backgroundImage = "url(" + dataUrl + ")";

            //Create grass header
            ctx.globalAlpha = 1;
            ctx.drawImage(sourceImageElement,
                headerBackgroundImage.x, headerBackgroundImage.y, headerBackgroundImage.width, headerBackgroundImage.height,
                0, 0, bgCanvas.width, bgCanvas.height
            );
            ctx.globalAlpha = 0.5;
            ctx.fillStyle = "black";
            ctx.fillRect(0, 0, bgCanvas.width, bgCanvas.height);
            dataUrl = bgCanvas.toDataURL();
            document.getElementById("headerContainer").style.backgroundImage = "url(" + dataUrl + ")";
        } catch(err) {
            console.log("Dynamic background not available in the current context");
            console.log(err);
            return;
        }
    }
}

function notifyOnImageComplete() {
    var img = document.getElementById("sheet")
    if (img.complete) {
        onTextureLoaded()
    } else {
        img.addEventListener('load', onTextureLoaded)
    }
}
