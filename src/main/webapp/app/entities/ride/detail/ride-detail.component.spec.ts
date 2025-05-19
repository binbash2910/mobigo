import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { RideDetailComponent } from './ride-detail.component';

describe('Ride Management Detail Component', () => {
  let comp: RideDetailComponent;
  let fixture: ComponentFixture<RideDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./ride-detail.component').then(m => m.RideDetailComponent),
              resolve: { ride: () => of({ id: 7624 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(RideDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RideDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load ride on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', RideDetailComponent);

      // THEN
      expect(instance.ride()).toEqual(expect.objectContaining({ id: 7624 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
