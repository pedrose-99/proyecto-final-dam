import shutil
from playwright.sync_api import sync_playwright, Playwright

CHROMIUM_PATH = (
    shutil.which("chromium")
    or shutil.which("chromium-browser")
    or shutil.which("google-chrome")
    or shutil.which("google-chrome-stable")
)


def launch_browser(playwright: Playwright):
    """Lanza Chromium con headless=new para pasar protecciones anti-bot.

    Returns (browser, context, page).
    """
    args = [
        "--headless=new",
        "--disable-blink-features=AutomationControlled",
        "--no-sandbox",
    ]

    if CHROMIUM_PATH:
        browser = playwright.chromium.launch(
            headless=False,
            executable_path=CHROMIUM_PATH,
            args=args,
        )
    else:
        # Usar Playwright-managed Chromium (Docker / CI)
        browser = playwright.chromium.launch(headless=True, args=args)

    context = browser.new_context(
        user_agent=(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        ),
        viewport={"width": 1920, "height": 1080},
        locale="es-ES",
    )
    page = context.new_page()
    page.add_init_script(
        'Object.defineProperty(navigator, "webdriver", {get: () => undefined});'
    )
    return browser, context, page
