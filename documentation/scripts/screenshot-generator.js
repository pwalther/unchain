import { fileURLToPath } from 'url';
import path from 'path';
import { chromium } from 'playwright';
import fs from 'fs';
import concurrently from 'concurrently';
import waitOn from 'wait-on';

// Configuration
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '../../');
const ADMIN_API_DIR = path.join(ROOT_DIR, 'admin-api');
const UI_DIR = path.join(ROOT_DIR, 'ui');
const OUTPUT_DIR = path.join(__dirname, '../screenshots');

// URLs to capture
const URLS_TO_CAPTURE = [
    '/dashboard',
    '/features',
    '/features/view/?featureName=SampleFlag&projectId=demo',
    '/projects',
    '/features',
    '/change-requests',
    '/history',
    '/metrics',
    '/strategies',
    '/settings',
    '/environments',
    '/environments/view/?name=Production',
    '/contexts'
];

const UI_BASE_URL = 'http://localhost:3005'; // Use non-default port, make sure this is in the list of allowed origins (application.yaml - unchain.security.cors.allowed-origins)
const API_PORT = 8082;

async function captureScreenshots() {
    if (!fs.existsSync(OUTPUT_DIR)) {
        fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    }

    console.log('Launching browser...');
    const browser = await chromium.launch();
    const page = await browser.newPage();

    // Set viewport size
    await page.setViewportSize({ width: 1280, height: 800 });

    for (const urlPath of URLS_TO_CAPTURE) {
        const fullUrl = `${UI_BASE_URL}${urlPath}`;
        console.log(`Navigating to ${fullUrl}...`);

        try {
            await page.goto(fullUrl, { waitUntil: 'networkidle' });

            // Generate filename from path
            const filename = urlPath.replace(/\//g, '_').replace(/^_/, '') || 'home';
            const filePath = path.join(OUTPUT_DIR, `${filename}.png`);

            await page.screenshot({ path: filePath, fullPage: true });
            console.log(`Saved screenshot: ${filePath}`);
        } catch (e) {
            console.error(`Failed to capture ${fullUrl}:`, e);
        }
    }

    await browser.close();
}

// Main execution
(async () => {
    let commands = [];

    // Clean shutdown handler
    const cleanup = () => {
        console.log('Cleaning up processes...');
        if (commands) {
            commands.forEach(cmd => {
                try {
                    cmd.kill('SIGTERM');
                } catch (e) {
                    console.error('Error killing command:', e);
                }
            });
        }
    };

    // Register signal handlers
    process.on('SIGINT', () => {
        console.log('\nReceived SIGINT. Cleaning up...');
        cleanup();
        process.exit();
    });

    process.on('SIGTERM', () => {
        console.log('\nReceived SIGTERM. Cleaning up...');
        cleanup();
        process.exit();
    });

    try {
        console.log('Starting services...');
        const mvnCmd = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
        const npmCmd = process.platform === 'win32' ? 'npm.cmd' : 'npm';
        const npxCmd = process.platform === 'win32' ? 'npx.cmd' : 'npx';

        const { result, commands: spawnedCommands } = concurrently(
            [
                {
                    command: `${mvnCmd} spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.jvmArguments="-Dserver.port=${API_PORT}"`,
                    name: 'BACKEND',
                    cwd: ADMIN_API_DIR,
                    prefixColor: 'blue'
                },
                {
                    command: `${npxCmd} wait-on tcp:localhost:${API_PORT} && ${npmCmd} run dev -- -p 3005`,
                    name: 'UI',
                    cwd: UI_DIR,
                    env: { ...process.env, NEXT_PUBLIC_API_URL: `http://localhost:${API_PORT}` },
                    prefixColor: 'magenta'
                }
            ],
            {
                killOthers: ['failure', 'success'],
            }
        );

        // Prevent unhandled rejection when processes are killed or fail
        result.catch(err => {
            // This is expected when we kill processes or if they fail early
        });

        commands = spawnedCommands;

        console.log('Waiting for services to be healthy...');
        await waitOn({
            resources: [
                `tcp:localhost:${API_PORT}`,
                `tcp:localhost:3005`
            ],
            timeout: 600000, // 10 minutes (Maven download/build can take time)
            interval: 2000
        });

        console.log('Services are up! Giving UI a moment to settle...');
        await new Promise(resolve => setTimeout(resolve, 5000));

        await captureScreenshots();

    } catch (error) {
        console.error('Script failed:', error);
    } finally {
        cleanup();

        // Force exit after a small delay to allow cleanup
        setTimeout(() => process.exit(0), 1000);
    }
})();
