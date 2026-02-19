import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, forkJoin, Subscription, timer } from 'rxjs';
import { AdminService } from './admin.service';

export interface StoreScrapingState {
    isRunning: boolean;
    lastScrapeStatus: string | null;
    lastScrapeTime: string | null;
}

@Injectable({ providedIn: 'root' })
export class ScrapingStateService implements OnDestroy
{
    private stateMap = new Map<string, BehaviorSubject<StoreScrapingState>>();
    private allStates = new BehaviorSubject<Map<string, StoreScrapingState>>(new Map());
    allStates$ = this.allStates.asObservable();

    private pollSub: Subscription | null = null;

    constructor(private adminService: AdminService) {}

    ngOnDestroy(): void
    {
        this.stopPolling();
    }

    initStores(slugs: string[]): void
    {
        for (const slug of slugs) {
            if (!this.stateMap.has(slug)) {
                this.stateMap.set(slug, new BehaviorSubject<StoreScrapingState>({
                    isRunning: false,
                    lastScrapeStatus: null,
                    lastScrapeTime: null
                }));
            }
        }
    }

    refreshAllStatuses(): void
    {
        const slugs = Array.from(this.stateMap.keys());
        if (slugs.length === 0) return;

        const requests: Record<string, ReturnType<AdminService['getStoreScrapingStatus']>> = {};
        for (const slug of slugs) {
            requests[slug] = this.adminService.getStoreScrapingStatus(slug);
        }

        forkJoin(requests).subscribe({
            next: (results) => {
                for (const slug of slugs) {
                    const status = results[slug];
                    if (status) {
                        const state: StoreScrapingState = {
                            isRunning: status.isRunning,
                            lastScrapeStatus: status.lastScrapeStatus,
                            lastScrapeTime: status.lastScrapeTime
                        };
                        this.stateMap.get(slug)!.next(state);
                    }
                }
                this.emitAll();
                this.managePoll();
            },
            error: () => {}
        });
    }

    markRunning(slug: string): void
    {
        const subject = this.stateMap.get(slug);
        if (subject) {
            subject.next({ ...subject.value, isRunning: true });
            this.emitAll();
            this.managePoll();
        }
    }

    markFinished(slug: string, status: string, time: string): void
    {
        const subject = this.stateMap.get(slug);
        if (subject) {
            subject.next({
                isRunning: false,
                lastScrapeStatus: status,
                lastScrapeTime: time
            });
            this.emitAll();
            this.managePoll();
        }
    }

    isAnyRunning(): boolean
    {
        for (const subject of this.stateMap.values()) {
            if (subject.value.isRunning) return true;
        }
        return false;
    }

    getStoreState(slug: string): StoreScrapingState | undefined
    {
        return this.stateMap.get(slug)?.value;
    }

    getStoreState$(slug: string): BehaviorSubject<StoreScrapingState> | undefined
    {
        return this.stateMap.get(slug);
    }

    private emitAll(): void
    {
        const snapshot = new Map<string, StoreScrapingState>();
        for (const [slug, subject] of this.stateMap) {
            snapshot.set(slug, subject.value);
        }
        this.allStates.next(snapshot);
    }

    private managePoll(): void
    {
        if (this.isAnyRunning() && !this.pollSub) {
            this.pollSub = timer(5000, 5000).subscribe(() => {
                this.refreshAllStatuses();
            });
        } else if (!this.isAnyRunning() && this.pollSub) {
            this.stopPolling();
        }
    }

    private stopPolling(): void
    {
        this.pollSub?.unsubscribe();
        this.pollSub = null;
    }
}
