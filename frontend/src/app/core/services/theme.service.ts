import { Injectable, signal, effect } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService
{
    private static readonly STORAGE_KEY = 'smartcart-dark-mode';

    isDark = signal(this.loadPreference());

    constructor()
    {
        effect(() =>
        {
            const dark = this.isDark();
            document.documentElement.classList.toggle('dark', dark);
            localStorage.setItem(ThemeService.STORAGE_KEY, JSON.stringify(dark));
        });
    }

    toggle(): void
    {
        this.isDark.update(v => !v);
    }

    private loadPreference(): boolean
    {
        const stored = localStorage.getItem(ThemeService.STORAGE_KEY);
        if (stored !== null)
        {
            return JSON.parse(stored);
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
}
