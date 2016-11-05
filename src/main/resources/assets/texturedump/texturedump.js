var cellSize = {x: 128, y: 128};
var cells = [];
var mods = {};
var lastCell;

var totalSheetPixels = 1;
var totalUsedPixels = 1;

var resourceDomainElement;
var nameElement;
var sizeElement;
var markerElement;
var imageElement;
var imageContainerElement;
var sourceImageElement;
var debugElement;
var resourceDomainsElement;
var resourceDomainOverlayElement;
var backgroundTypesElement;
var modNameElement;
var mapPixelPercentElement;
var sheetPixelPercentElement;
var modPercentageElement;

function initialize() {
    var sheet = document.getElementById("sheet");

    var cellDimensions = {
        width: Math.ceil(sheet.width / cellSize.x),
        height: Math.ceil(sheet.height / cellSize.y)
    };
    totalSheetPixels = sheet.width * sheet.height;
    totalUsedPixels = 0;

    //Proccess mod statistics
    for (var i = 0; i < modStatistics.length; i++) {
        var mod = modStatistics[i];
        if (mods.hasOwnProperty(mod.resourceDomain)) {
            console.log("resourceDomain '" + mod.resourceDomain + "' is listed multiple times.");
            continue;
        }

        mods[mod.resourceDomain] = mod;
        mods[mod.resourceDomain].tiles = [];
        mods[mod.resourceDomain].totalTilePixels = 0;
    }

    //Preallocate Cells for speed
    cells = [];
    for (var y = 0; y <= cellDimensions.height; ++y) {
        cells[y] = [];
        for (var x = 0; x <= cellDimensions.width; ++x) {
            cells[y][x] = []
        }
    }

    //Add sprite indexes to all the cells they occupy
    for (var i = 0; i < textureData.length; i++) {
        var sprite = textureData[i];

        var cellMinY = Math.floor(sprite.y / cellSize.y);
        var cellMaxY = Math.ceil((sprite.y + sprite.height) / cellSize.y);
        var cellMinX = Math.floor(sprite.x / cellSize.x);
        var cellMaxX = Math.ceil((sprite.x + sprite.width) / cellSize.x);
        for (var y = cellMinY; y <= cellMaxY; ++y) {
            for (var x = cellMinX; x <= cellMaxX; ++x) {
                cells[y][x].push(i);
            }
        }

        var resourceDomain = sprite.name.substring(0, sprite.name.indexOf(":"));
        if (mods.hasOwnProperty(resourceDomain) && mods[resourceDomain].tiles !== undefined) {
            mods[resourceDomain].tiles.push(i);
            mods[resourceDomain].totalTilePixels += textureData[i].width * textureData[i].height
        }

        totalUsedPixels += sprite.width * sprite.height;
    }

    //Pre-resolve elements that will be updated
    resourceDomainElement = document.getElementById("resourceDomain");
    modNameElement = document.getElementById("modName");
    nameElement = document.getElementById("name");
    sizeElement = document.getElementById("size");
    markerElement = document.getElementById("marker");
    imageElement = document.getElementById("singleImage");
    imageContainerElement = document.getElementById("singleImageContainer");
    debugElement = document.getElementById("debug");
    sourceImageElement = document.getElementById("sheet");
    resourceDomainsElement = document.getElementById("resourceDomains");
    backgroundTypesElement = document.getElementById("backgroundTypes");
    resourceDomainOverlayElement = document.getElementById("resourceDomainOverlay");
    mapPixelPercentElement = document.getElementById("mapPixelPercent");
    sheetPixelPercentElement = document.getElementById("sheetPixelPercent");
    modPercentageElement = document.getElementById("modPercentage");

    resourceDomainsElement.appendChild(document.createElement("option"));
    for(var resourceDomain in mods){
        console.log(resourceDomain + ': ' + mods[resourceDomain].modName);
        var newOption = document.createElement("option");
        newOption.value = resourceDomain;
        newOption.innerHTML = mods[resourceDomain].modName
        resourceDomainsElement.appendChild(newOption)
    }

    document.getElementById("loading").style.display = "none";

    resourceDomainOverlayElement.width = sheet.width;
    resourceDomainOverlayElement.height = sheet.height;

    sheet.addEventListener("mousemove", onMouseMove);
    resourceDomainOverlayElement.addEventListener("mousemove", onMouseMove);
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
            var textureDataIndex = -1;
            for (var i = 0; i < containedSprites.length; ++i) {
                var data = textureData[containedSprites[i]];
                if (offsetX >= data.x && offsetX <= data.x + data.width) {
                    if (offsetY >= data.y && offsetY <= data.y + data.height) {
                        textureDataIndex = containedSprites[i];
                        break;
                    }
                }
            }

            if (textureDataIndex < 0) {
                updateSingleImage(null);

                fastdom.mutate(function() {
                    nameElement.innerText = "";
                    sizeElement.innerText = "";
                    modNameElement.innerText = "";
                    mapPixelPercentElement.innerText = "";
                    sheetPixelPercentElement.innerText = "";
                    markerElement.style.display = "none";
                });

                lastCell = -1;
                return;
            }

            if (textureDataIndex != lastCell) {
                lastCell = textureDataIndex;
                var data = textureData[textureDataIndex];

                var targetTop = eventInfo.target.offsetTop;
                var targetLeft = eventInfo.target.offsetLeft;

                updateSingleImage(data);

                var resourceDomain = data.name.substring(0, data.name.indexOf(":"));
                var modName = mods[resourceDomain].modName;

                fastdom.mutate(function() {
                    modNameElement.innerText = modName;
                    nameElement.innerText = data.name;
                    sizeElement.innerText = data.width + " x " + data.height;

                    markerElement.style.top = data.y + targetTop;
                    markerElement.style.left = data.x + targetLeft;
                    markerElement.style.width = data.width-2;
                    markerElement.style.height = data.height-2;
                    markerElement.style.display = "block";

                    var tilePixels = data.width * data.height;
                    var usedMapPercent = Math.floor((tilePixels / totalUsedPixels) * 10000) / 100;
                    mapPixelPercentElement.innerText = (usedMapPercent < 0.01 ? "< 1" : usedMapPercent) + "%";
                });
            }
        } else {
            updateSingleImage(null);

            fastdom.mutate(function() {
                nameElement.innerText = "";
                sizeElement.innerText = "";
                modNameElement.innerText = "";
                mapPixelPercentElement.innerText = "";
                markerElement.style.display = "none";

            });
            lastCell = -1;
        }
    });
}

function updateSingleImage(data) {
    var imageElementWidth = imageElement.width;
    var imageElementHeight = imageElement.height;

    var ctx = imageElement.getContext("2d");
    ctx.mozImageSmoothingEnabled = false;
    ctx.msImageSmoothingEnabled = false;
    ctx.imageSmoothingEnabled = false;

    ctx.clearRect(0, 0, imageElementWidth, imageElementHeight);

    if (data != null) {
        ctx.globalOpacity = 1;
        ctx.drawImage(sourceImageElement,
            data.x, data.y, data.width, data.height,
            0, 0, imageElementWidth, imageElementHeight
        );
    }
}

function highlightResourceDomain() {
    var resourceDomain = resourceDomainsElement.options[resourceDomainsElement.selectedIndex].value;
    console.log(resourceDomain);

    var ctx = resourceDomainOverlayElement.getContext("2d");
    ctx.clearRect(0, 0, resourceDomainOverlayElement.width, resourceDomainOverlayElement.height);
    if (mods.hasOwnProperty(resourceDomain)) {
        var mod = mods[resourceDomain];

        ctx.mozImageSmoothingEnabled = false;
        ctx.msImageSmoothingEnabled = false;
        ctx.imageSmoothingEnabled = false;

        ctx.globalAlpha = 0.8;
        ctx.fillStyle = "white";
        ctx.fillRect(0, 0, resourceDomainOverlayElement.width, resourceDomainOverlayElement.height);

        for (var i = 0; i < mod.tiles.length; ++i) {
            var tile = textureData[mod.tiles[i]];
            ctx.clearRect(tile.x, tile.y, tile.width, tile.height);
        }

        var usedMapPercent = Math.floor((mod.totalTilePixels / totalUsedPixels) * 10000) / 100;
        modPercentageElement.innerText = usedMapPercent + "% of map";
    } else {
        modPercentageElement.innerText = "";
    }
}

function updateBackground() {
    sourceImageElement.style.background = backgroundTypesElement.value;
    imageContainerElement.style.background = backgroundTypesElement.value;
}

function onTextureLoaded() {
    initialize();

    var backgroundImage;
    var headerBackgroundImage;

    for (var i = 0; i < textureData.length; i++) {
        if (textureData[i].name == "minecraft:blocks/dirt") {
            backgroundImage = textureData[i];
        }
        if (textureData[i].name == "minecraft:blocks/grass_side") {
            headerBackgroundImage = textureData[i];
        }
    }

    if (backgroundImage != null) {
        try {
            var sourceImageElement = document.getElementById("sheet");
            var dataUrl;

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
            console.log("Dynamic background not available in the current context because:");
            console.log(err);
        }
    }
}

function notifyOnImageComplete() {
    var img = document.getElementById("sheet");
    if (img.complete) {
        onTextureLoaded()
    } else {
        img.addEventListener('load', onTextureLoaded)
    }
}
