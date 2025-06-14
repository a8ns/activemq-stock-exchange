# Use official Apache ActiveMQ Classic image
FROM apache/activemq-classic:latest

# Expose ports
EXPOSE 61616 8161 5672 61613 1883

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8161/ || exit 1
