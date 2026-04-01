#!/bin/bash

# Script para actualizar el contenedor Docker de Records API con los últimos cambios
# Uso: ./update-docker.sh

echo "=========================================="
echo "🔄 Actualizando Records API en Docker"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Paso 1: Detener contenedor actual
echo -e "${BLUE}1. Deteniendo contenedor actual...${NC}"
docker-compose down records-api 2>/dev/null || echo "No había contenedor corriendo"
echo ""

# Paso 2: Limpiar imágenes antiguas (opcional)
echo -e "${YELLOW}2. ¿Deseas limpiar imágenes antiguas? (y/n)${NC}"
read -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    echo -e "${BLUE}   Limpiando imágenes antiguas...${NC}"
    docker images | grep records-api | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
fi
echo ""

# Paso 3: Construir nueva imagen
echo -e "${BLUE}3. Construyendo nueva imagen con los últimos cambios...${NC}"
docker-compose build --no-cache records-api

if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✅ Imagen construida exitosamente${NC}"
else
    echo -e "${RED}   ❌ Error al construir la imagen${NC}"
    exit 1
fi
echo ""

# Paso 4: Iniciar contenedor
echo -e "${BLUE}4. Iniciando contenedor actualizado...${NC}"
docker-compose up -d records-api

if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✅ Contenedor iniciado${NC}"
else
    echo -e "${RED}   ❌ Error al iniciar el contenedor${NC}"
    exit 1
fi
echo ""

# Paso 5: Esperar a que la aplicación inicie
echo -e "${BLUE}5. Esperando a que la aplicación inicie...${NC}"
sleep 5

# Paso 6: Verificar estado
echo -e "${BLUE}6. Verificando estado del contenedor...${NC}"
docker-compose ps records-api
echo ""

# Paso 7: Health check
echo -e "${BLUE}7. Verificando health check...${NC}"
for i in {1..10}; do
    if curl -s http://localhost:8082/actuator/health | grep -q "UP"; then
        echo -e "${GREEN}   ✅ API está respondiendo correctamente${NC}"
        echo ""
        break
    else
        echo -e "${YELLOW}   ⏳ Esperando... (intento $i/10)${NC}"
        sleep 2
    fi

    if [ $i -eq 10 ]; then
        echo -e "${RED}   ❌ La API no respondió después de 10 intentos${NC}"
        echo -e "${YELLOW}   Ver logs con: docker-compose logs -f records-api${NC}"
        exit 1
    fi
done

# Paso 8: Mostrar información útil
echo "=========================================="
echo -e "${GREEN}✅ ACTUALIZACIÓN COMPLETADA${NC}"
echo "=========================================="
echo ""
echo "📊 Información útil:"
echo "  • API URL: http://localhost:8082"
echo "  • Health check: http://localhost:8082/actuator/health"
echo "  • Ver logs: docker-compose logs -f records-api"
echo "  • Detener: docker-compose down records-api"
echo "  • Reiniciar: docker-compose restart records-api"
echo ""
echo "🎯 Probar los aspectos AOP:"
echo "  ./test-aop.sh"
echo ""
echo "📚 Documentación:"
echo "  • README.md"
echo "  • QUICKSTART.md"
echo "  • AOP-QUICKSTART.md"
echo ""

