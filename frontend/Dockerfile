# ═══════════════════════════════════════════════════════════════
# Dockerfile — Frontend (Angular + Nginx)
#
# Pattern : build once, promote always
# Le dist Angular est compilé et linté par le CI (GitHub Actions Job 2)
# puis téléchargé dans frontend/dist/ avant docker build.
# Ce Dockerfile ne recompile rien — il empaquète uniquement.
# ═══════════════════════════════════════════════════════════════

FROM nginx:alpine

# ─── Sécurité : utilisateur non-root ───────────────────────────
RUN addgroup -S app && adduser -S app -G app

# ─── Artefact fourni par le CI ─────────────────────────────────
COPY dist/todo-frontend/browser /usr/share/nginx/html

# ─── Config Nginx personnalisée (fallback SPA Angular) ─────────
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R app:app /usr/share/nginx/html

# ─── Exposition du port HTTP ────────────────────────────────────
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]