# =============================================================================
# APG C SDK Makefile
# =============================================================================

# Compiler settings
CC = gcc
CFLAGS = -std=c11 -Wall -Wextra -Werror -O2 -fPIC
CFLAGS_DEBUG = -std=c11 -Wall -Wextra -g -O0 -DDEBUG

# Auto-detect Homebrew paths for dependencies
CJSON_PREFIX := $(shell brew --prefix cjson 2>/dev/null || echo /usr/local)
CURL_PREFIX := $(shell brew --prefix curl 2>/dev/null || echo /usr/local)

INCLUDES = -I. -I./src -I./src/autogen -I$(CJSON_PREFIX)/include -I$(CURL_PREFIX)/include

# Linker settings
LDFLAGS = -shared -L$(CJSON_PREFIX)/lib -L$(CURL_PREFIX)/lib
LDLIBS = -lcjson -lcurl

# Directories
SRC_DIR = ./src
AUTOGEN_DIR = ./src/autogen
CLIENT_DIR = ./client
BUILD_DIR = build
OBJ_DIR = $(BUILD_DIR)/obj
LIB_DIR = $(BUILD_DIR)/lib
BIN_DIR = $(BUILD_DIR)/bin

# Source files (preset in src/ + generated in src/autogen/)
PRESET_SOURCES = $(wildcard $(SRC_DIR)/*.c)
AUTOGEN_SOURCES = $(wildcard $(AUTOGEN_DIR)/*.c)
SOURCES = $(PRESET_SOURCES) $(AUTOGEN_SOURCES)
OBJECTS = $(patsubst $(SRC_DIR)/%.c,$(OBJ_DIR)/%.o,$(PRESET_SOURCES)) \
          $(patsubst $(AUTOGEN_DIR)/%.c,$(OBJ_DIR)/autogen_%.o,$(AUTOGEN_SOURCES))

# Client sources
CLIENT_SRC_DIR = $(CLIENT_DIR)/src
CLIENT_SOURCES = $(wildcard $(CLIENT_SRC_DIR)/*.c)
CLIENT_OBJECTS = $(patsubst $(CLIENT_SRC_DIR)/%.c,$(OBJ_DIR)/client_%.o,$(CLIENT_SOURCES))

# Library names
STATIC_LIB = $(LIB_DIR)/libapg_sdk.a
SHARED_LIB = $(LIB_DIR)/libapg_sdk.so
CLIENT_STATIC_LIB = $(LIB_DIR)/libapg_client.a
CLIENT_SHARED_LIB = $(LIB_DIR)/libapg_client.so

# Test directory and sources (auto-discover all test files)
TEST_DIR = $(CLIENT_DIR)/test
TEST_SOURCES = $(wildcard $(TEST_DIR)/*.c)
TEST_TARGETS = $(patsubst $(TEST_DIR)/%.c,$(BIN_DIR)/%,$(TEST_SOURCES))

# Default target
.PHONY: all clean install debug test client tests

all: dirs $(STATIC_LIB) $(SHARED_LIB)

# Create directories
dirs:
	@mkdir -p $(OBJ_DIR) $(LIB_DIR) $(BIN_DIR)

# Compile preset source files (src/*.c)
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c
	@echo "CC [PRESET] $<"
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# Compile autogen source files (src/autogen/*.c)
$(OBJ_DIR)/autogen_%.o: $(AUTOGEN_DIR)/%.c
	@echo "CC [AUTOGEN] $<"
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# Compile client source files
$(OBJ_DIR)/client_%.o: $(CLIENT_SRC_DIR)/%.c
	@echo "CC [CLIENT] $<"
	@$(CC) $(CFLAGS) $(INCLUDES) -I$(CLIENT_DIR)/include -c $< -o $@

# Create static library
$(STATIC_LIB): $(OBJECTS)
	@echo "AR $@"
	@ar rcs $@ $^

# Create shared library
$(SHARED_LIB): $(OBJECTS)
	@echo "LD $@"
	@$(CC) $(LDFLAGS) -o $@ $^ $(LDLIBS)

# Create client static library
$(CLIENT_STATIC_LIB): $(CLIENT_OBJECTS)
	@echo "AR $@"
	@ar rcs $@ $^

# Create client shared library
$(CLIENT_SHARED_LIB): $(CLIENT_OBJECTS)
	@echo "LD $@"
	@$(CC) $(LDFLAGS) -o $@ $^ $(LDLIBS)

# Build client libraries
client: all $(CLIENT_STATIC_LIB) $(CLIENT_SHARED_LIB)

# Build all tests
tests: client $(TEST_TARGETS)

# Pattern rule for building any test
$(BIN_DIR)/%: $(TEST_DIR)/%.c $(STATIC_LIB) $(CLIENT_STATIC_LIB)
	@echo "CC [TEST] $<"
	@$(CC) $(CFLAGS) $(INCLUDES) -I$(CLIENT_DIR)/include -o $@ $< -L$(LIB_DIR) -lapg_sdk -lapg_client -L$(CJSON_PREFIX)/lib -L$(CURL_PREFIX)/lib -lcjson -lcurl

# Alias: 'test' runs all tests
test: tests

# Debug build
debug: CFLAGS = $(CFLAGS_DEBUG)
debug: clean all

# Clean build artifacts
clean:
	@echo "Cleaning build directory..."
	@rm -rf $(BUILD_DIR)

# Install library and headers
PREFIX ?= /usr/local
INSTALL_INC_DIR = $(PREFIX)/include/apg_sdk
INSTALL_LIB_DIR = $(PREFIX)/lib

install: all
	@echo "Installing APG SDK..."
	@mkdir -p $(INSTALL_INC_DIR) $(INSTALL_LIB_DIR)
	@cp $(SRC_DIR)/*.h $(INSTALL_INC_DIR)/
	@cp $(STATIC_LIB) $(INSTALL_LIB_DIR)/
	@cp $(SHARED_LIB) $(INSTALL_LIB_DIR)/
	@echo "Installed to $(PREFIX)"
	@echo "Headers: $(INSTALL_INC_DIR)"
	@echo "Libraries: $(INSTALL_LIB_DIR)"

install-client: client
	@echo "Installing APG Client..."
	@mkdir -p $(INSTALL_INC_DIR) $(INSTALL_LIB_DIR)
	@cp $(CLIENT_DIR)/include/*.h $(INSTALL_INC_DIR)/
	@cp $(CLIENT_STATIC_LIB) $(INSTALL_LIB_DIR)/
	@cp $(CLIENT_SHARED_LIB) $(INSTALL_LIB_DIR)/
	@echo "Installed client to $(PREFIX)"

uninstall:
	@echo "Uninstalling APG SDK..."
	@rm -rf $(INSTALL_INC_DIR)
	@rm -f $(INSTALL_LIB_DIR)/libapg_sdk.a
	@rm -f $(INSTALL_LIB_DIR)/libapg_sdk.so
	@rm -f $(INSTALL_LIB_DIR)/libapg_client.a
	@rm -f $(INSTALL_LIB_DIR)/libapg_client.so
	@echo "Uninstalled from $(PREFIX)"

# Print build info
info:
	@echo "APG C SDK Build Information"
	@echo "============================"
	@echo "Compiler: $(CC)"
	@echo "CFLAGS: $(CFLAGS)"
	@echo "Sources: $(SOURCES)"
	@echo "Objects: $(OBJECTS)"
	@echo "Static Lib: $(STATIC_LIB)"
	@echo "Shared Lib: $(SHARED_LIB)"

# Help
help:
	@echo "APG C SDK Makefile"
	@echo "=================="
	@echo ""
	@echo "Targets:"
	@echo "  all          - Build SDK static and shared libraries (default)"
	@echo "  client       - Build client libraries"
	@echo "  client-test  - Build client test executable"
	@echo "  debug        - Build with debug symbols"
	@echo "  example      - Build example client"
	@echo "  test         - Build test client"
	@echo "  clean        - Remove build artifacts"
	@echo "  install      - Install SDK library and headers"
	@echo "  install-client - Install client library and headers"
	@echo "  uninstall    - Remove installed files"
	@echo "  info         - Print build information"
	@echo "  help         - Show this help message"
	@echo ""
	@echo "Variables:"
	@echo "  CC       - Compiler (default: gcc)"
	@echo "  PREFIX   - Install prefix (default: /usr/local)"
	@echo ""
	@echo "Examples:"
	@echo "  make                    # Build SDK libraries"
	@echo "  make client             # Build client libraries"
	@echo "  make client-test        # Build client tests"
	@echo "  make debug              # Build with debug info"
	@echo "  make example            # Build example client"
	@echo "  make PREFIX=/opt/apg install  # Install to /opt/apg"
