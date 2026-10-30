package com.kaviyo.app;

/** Renders Kaviyo's editorial landing page and scroll-linked product-discovery story. */
public final class HomePageRenderer {
    public String render() {
        StringBuilder html = new StringBuilder(22_000);
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light">
                  <meta name="description" content="Kaviyo helps merchants understand how product facts support the needs shoppers bring to AI agents.">
                  <title>Kaviyo | Product facts for AI shopping</title>
                  <style>
                """);
        html.append(HtmlReportRenderer.STYLES);
        html.append("""
                  .home-page {
                    --color-parchment: #fefffc;
                    --color-paper: #ffffff;
                    --color-linen: #f9faf7;
                    --color-ink-black: #171717;
                    --color-graphite: #2c2c2c;
                    --color-charcoal: #444141;
                    --color-ash: #646464;
                    --color-fog: #b4b8b4;
                    --color-mist: #dee2de;
                    --color-twilight: #282834;
                    --color-dusk: #1f1f29;
                    --color-signal-blue: #41a1cf;
                    --color-cerulean: #0081c0;
                    --canvas: var(--color-parchment);
                    --paper: var(--color-paper);
                    --ink: var(--color-ink-black);
                    --muted: var(--color-ash);
                    --quiet: #858985;
                    --line: var(--color-mist);
                    color: var(--ink);
                    background: var(--color-parchment);
                    font-family: Inter, "Helvetica Neue", Arial, sans-serif;
                  }
                  .home-page[data-theme="dark"] {
                    --color-parchment: #1f1f29; --color-paper: #282834; --color-linen: #302e3b;
                    --color-ink-black: #f7f6f5; --color-graphite: #ebe9e9; --color-charcoal: #d6d2d5;
                    --color-ash: #c0bcc2; --color-fog: #77727d; --color-mist: #494652;
                    --canvas: var(--color-parchment); --paper: var(--color-paper); --ink: var(--color-ink-black);
                    --muted: var(--color-ash); --quiet: #aaa5af; --line: var(--color-mist);
                  }
                  .home-page[data-theme="dark"] .home-nav .nav-action,
                  .home-page[data-theme="dark"] .home-close .home-primary { color: #171717; }
                  .home-page h1, .home-page h2, .home-page h3 {
                    font-family: "ppmondwest", "Iowan Old Style", Georgia, serif;
                    font-weight: 400;
                    letter-spacing: -.045em;
                  }
                  .home-page .home-main { width: 100%; max-width: none; margin: 0; }
                  .home-page .eyebrow { color: var(--color-cerulean); font-size: 12px; font-weight: 600; letter-spacing: .035em; text-transform: none; }
                  .home-nav {
                    position: fixed;
                    top: 22px;
                    left: 50%;
                    z-index: 20;
                    display: grid;
                    grid-template-columns: 1fr auto 1fr;
                    align-items: center;
                    width: min(930px, calc(100% - 48px));
                    min-height: 58px;
                    padding: 6px 8px 6px 19px;
                    border: 1px solid rgba(255,255,255,.36);
                    border-radius: 999px;
                    background: rgba(31,31,41,.76);
                    color: #fff;
                    transform: translateX(-50%);
                    backdrop-filter: blur(18px);
                    -webkit-backdrop-filter: blur(18px);
                    box-shadow: 0 8px 28px rgba(31,31,41,.12);
                  }
                  .home-hero {
                    position: sticky;
                    top: 16px;
                    height: calc(100svh - 32px);
                    min-height: min(590px, calc(100svh - 32px));
                    margin: 16px;
                    overflow: hidden;
                    border-radius: 30px;
                    isolation: isolate;
                    background: var(--color-twilight);
                    color: #fff;
                  }
                  .hero-landscape, .hero-veil { position: absolute; inset: 0; width: 100%; height: 100%; }
                  .hero-landscape { z-index: -2; object-fit: cover; transform: scale(var(--hero-scale, 1)) translateY(var(--hero-shift, 0px)); transform-origin: 50% 52%; will-change: transform; }
                  .hero-landscape .city-layer { transform-box: fill-box; transform-origin: center bottom; will-change: transform; }
                  .hero-landscape .city-layer--distant { transform: translate(var(--city-distant-x, 0px), var(--city-distant-y, 0px)); }
                  .hero-landscape .city-layer--front { transform: translate(var(--city-front-x, 0px), var(--city-front-y, 0px)) scale(var(--city-front-scale, 1)); }
                  .hero-landscape .traffic-flow { will-change: transform; }
                  .hero-veil {
                    z-index: -1;
                    background: linear-gradient(90deg, rgba(25,25,39,.26), transparent 70%),
                      linear-gradient(0deg, rgba(25,24,37,.44), transparent 72%);
                  }
                  .home-brand { justify-self: start; color: inherit; font-size: 20px; font-weight: 650; letter-spacing: -.065em; text-decoration: none; }
                  .home-nav nav { display: flex; align-items: center; gap: 5px; }
                  .home-nav nav a { padding: 9px 13px; border-radius: 999px; color: rgba(255,255,255,.9); font-size: 12px; text-decoration: none; transition: background-color .2s ease; }
                  .home-nav nav a:hover { background: rgba(255,255,255,.14); }
                  .theme-toggle { display: inline-grid; width: 34px; height: 34px; place-items: center; border: 1px solid rgba(255,255,255,.28); border-radius: 50%; background: transparent; color: inherit; font-size: 17px; cursor: pointer; }
                  .theme-toggle:focus-visible { outline: 2px solid #8dd7ef; outline-offset: 3px; }
                  .home-nav .nav-action {
                    justify-self: end;
                    display: inline-flex;
                    align-items: center;
                    gap: 10px;
                    padding: 10px 15px;
                    border: 1px solid rgba(255,255,255,.26);
                    border-radius: 999px;
                    background: rgba(255,255,255,.93);
                    color: var(--color-ink-black);
                    font-size: 12px;
                    font-weight: 600;
                    text-decoration: none;
                  }
                  .home-nav .nav-action span { font-size: 15px; }
                  .home-nav a:focus-visible, .hero-story a:focus-visible, .scroll-cue:focus-visible, .home-page .quiet-link:focus-visible {
                    outline: 2px solid #fff;
                    outline-offset: 4px;
                  }
                  .hero-sequence { min-height: 170svh; position: relative; }
                  .hero-story {
                    position: absolute;
                    bottom: clamp(32px, 7vh, 76px);
                    left: clamp(24px, 5vw, 82px);
                    width: min(570px, calc(100% - 48px));
                    padding: clamp(23px, 3vw, 39px);
                    border: 1px solid rgba(255,255,255,.25);
                    border-radius: 20px;
                    background: rgba(31,30,43,.38);
                    backdrop-filter: blur(18px);
                    -webkit-backdrop-filter: blur(18px);
                    opacity: var(--hero-copy-opacity, 1);
                    transform: translateY(var(--hero-copy-shift, 0px));
                    will-change: transform, opacity;
                  }
                  .hero-kicker { margin: 0 0 13px; color: rgba(255,255,255,.86); font-size: 13px; letter-spacing: .015em; }
                  .hero-story h1 { max-width: 520px; margin: 0 0 14px; font-size: clamp(42px, 5.5vw, 69px); line-height: .99; }
                  .hero-story > p:not(.hero-kicker):not(.hero-truth) { max-width: 460px; margin: 0; color: rgba(255,255,255,.89); font-size: 14px; line-height: 1.6; }
                  .hero-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 19px; margin-top: 23px; }
                  .hero-primary { display: inline-flex; align-items: center; gap: 15px; min-height: 42px; padding: 10px 14px; border-radius: 4px; background: #fff; color: var(--color-ink-black); font-size: 12px; font-weight: 600; text-decoration: none; }
                  .hero-primary span { font-size: 17px; }
                  .hero-secondary { color: #fff; font-size: 12px; text-decoration: underline; text-decoration-color: rgba(255,255,255,.55); text-underline-offset: 4px; }
                  .hero-truth { max-width: 460px; margin: 19px 0 0; color: rgba(255,255,255,.73); font-size: 10px; line-height: 1.55; }
                  .hero-notification {
                    position: absolute;
                    top: clamp(100px, 16vh, 172px);
                    right: clamp(22px, 5vw, 76px);
                    z-index: 2;
                    display: grid;
                    grid-template-columns: 34px minmax(0, 1fr) 28px;
                    align-items: center;
                    gap: 12px;
                    width: min(390px, calc(100% - 44px));
                    padding: 13px 14px;
                    border: 1px solid rgba(255,255,255,.44);
                    border-radius: 10px;
                    background: rgba(250,252,250,.94);
                    color: #171717;
                    box-shadow: 0 18px 46px rgba(23,23,23,.2);
                    opacity: 0;
                    transform: translate3d(24px, -8px, 0) scale(.98);
                    animation: notice-arrive .72s cubic-bezier(.18,.72,.22,1) .35s forwards;
                  }
                  @keyframes notice-arrive { to { opacity: 1; transform: translate3d(0, 0, 0) scale(1); } }
                  .notification-mark { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 7px; background: #e5f3f7; color: #087da5; font-size: 15px; font-weight: 700; }
                  .notification-copy { min-width: 0; }
                  .notification-label { display: block; margin-bottom: 3px; color: #087da5; font-size: 9px; font-weight: 650; letter-spacing: .04em; text-transform: uppercase; }
                  .notification-copy strong { display: block; margin-bottom: 2px; font-size: 12px; font-weight: 650; }
                  .notification-copy small { display: block; overflow: hidden; color: #646464; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
                  .notification-open { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 5px; background: #171717; color: #fff; font-size: 14px; text-decoration: none; }
                  .hero-notification:focus-within { outline: 2px solid #fff; outline-offset: 3px; }
                  .scroll-cue { position: absolute; right: 42px; bottom: 36px; display: inline-flex; align-items: center; gap: 10px; color: #fff; font-size: 11px; text-decoration: none; }
                  .scroll-cue span { width: 23px; height: 23px; border: 1px solid rgba(255,255,255,.65); border-radius: 50%; background: linear-gradient(transparent 48%, rgba(255,255,255,.8) 49% 53%, transparent 54%) center/8px 8px no-repeat; }
                  .hero-caption { position: absolute; top: 50%; right: 22px; color: rgba(255,255,255,.75); font-size: 9px; letter-spacing: .04em; writing-mode: vertical-rl; }

                  .home-growth { width: 100%; margin: 0; padding: clamp(90px, 12vw, 160px) 0 0; }
                  .growth-intro { width: min(1280px, calc(100% - 104px)); margin: 0 auto clamp(50px, 9vw, 110px); }
                  .growth-intro .eyebrow { margin-bottom: 18px; }
                  .growth-intro h2 { max-width: 940px; margin: 0 0 18px; font-size: clamp(43px, 6.6vw, 82px); line-height: .98; }
                  .growth-intro > p:last-child { max-width: 650px; margin: 0; color: var(--color-ash); font-size: 16px; line-height: 1.7; }
                  .growth-track { position: relative; min-height: 300svh; color: #fff; background: #0081c0; isolation: isolate; }
                  .growth-scene { position: sticky; top: 0; z-index: 0; display: grid; height: 100svh; min-height: 0; overflow: hidden; place-items: center; margin-bottom: -100svh; background: #168fc7; }
                  .growth-sky { position: absolute; inset: 0; width: 100%; height: 100%; }
                  .growth-clouds { transform: translateX(var(--cloud-shift, 0px)); }
                  .growth-sun { transform: translateY(var(--sun-shift, 0px)); }
                  .growth-counter { position: absolute; right: clamp(28px, 7vw, 100px); bottom: clamp(25px, 5vh, 55px); z-index: 2; color: rgba(255,255,255,.87); font-size: 11px; letter-spacing: .08em; }
                  .pixel-tree { position: absolute; right: clamp(2vw, 8vw, 130px); bottom: -4vh; width: min(59vw, 900px); height: min(94vh, 880px); overflow: visible; }
                  .tree-stage { opacity: 0; transform: translateY(40px) scale(.92); transform-origin: 55% 90%; transition: opacity .7s ease, transform 1s cubic-bezier(.2,.7,.15,1); }
                  .growth-scene[data-growth="1"] .tree-stage-one,
                  .growth-scene[data-growth="2"] .tree-stage-two,
                  .growth-scene[data-growth="3"] .tree-stage-two,
                  .growth-scene[data-growth="3"] .tree-stage-three { opacity: 1; transform: translateY(0) scale(1); }
                  .growth-steps { position: relative; z-index: 1; pointer-events: none; }
                  .growth-step { display: flex; min-height: 100svh; align-items: center; padding: 80px 0; }
                  .growth-step-inner { width: min(485px, 42vw); margin-left: max(7vw, calc((100vw - 1280px)/2)); padding: clamp(24px, 3.6vw, 48px); border: 1px solid rgba(255,255,255,.13); border-radius: 4px; background: rgba(9,67,99,.28); box-shadow: 0 18px 70px rgba(6,57,91,.1); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); opacity: .03; transform: translateY(115px); transition: opacity .5s ease, transform .7s cubic-bezier(.2,.7,.15,1); }
                  .growth-step.is-active .growth-step-inner { opacity: 1; transform: translateY(78px); }
                  .growth-step .eyebrow { margin: 0 0 15px; color: #fff3a1; }
                  .growth-step h3 { margin: 0 0 15px; font-size: clamp(34px, 4.4vw, 60px); line-height: .99; }
                  .growth-step-inner > p:last-child { max-width: 420px; margin: 0; color: rgba(255,255,255,.88); font-size: 14px; line-height: 1.7; }
                  .growth-disclosure { width: min(1280px, calc(100% - 104px)); margin: 0 auto; padding: 22px 0 25px; color: var(--color-ash); font-size: 11px; line-height: 1.6; }

                  .discovery-flow { width: min(1280px, calc(100% - 104px)); margin: 0 auto; padding: clamp(76px, 10vw, 130px) 0; }
                  .discovery-flow-heading { display: grid; grid-template-columns: minmax(0, 1fr) minmax(250px, .55fr); align-items: end; gap: 28px; margin-bottom: 42px; }
                  .discovery-flow-heading .eyebrow { margin: 0 0 14px; }
                  .discovery-flow-heading h2 { max-width: 770px; margin: 0; font-size: clamp(42px, 6vw, 74px); line-height: .98; }
                  .discovery-flow-heading > p { margin: 0 0 3px; color: var(--color-ash); font-size: 14px; line-height: 1.7; }
                  .flow-board { --flow-grid-x: 0px; --flow-grid-y: 0px; position: relative; overflow: hidden; padding: clamp(20px, 3.5vw, 44px); border: 1px solid #dfe5e2; border-radius: 16px; background-color: #f8faf8; background-image: radial-gradient(#bdc9c9 1px, transparent 1.25px); background-position: calc(50% + var(--flow-grid-x)) calc(50% + var(--flow-grid-y)); background-size: 22px 22px; }
                  .flow-connectors { position: absolute; inset: 0; z-index: 0; width: 100%; height: 100%; pointer-events: none; }
                  .flow-connectors path { fill: none; stroke: #8da8ad; stroke-width: 2; stroke-dasharray: 5 7; vector-effect: non-scaling-stroke; }
                  .flow-stages { position: relative; z-index: 1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: clamp(18px, 2.5vw, 34px); }
                  .flow-stage { min-width: 0; }
                  .flow-stage-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 34px; margin-bottom: 21px; padding-bottom: 12px; border-bottom: 1px solid #cfd9d8; }
                  .flow-stage-heading h3 { margin: 0; color: #252b2b; font-size: 13px; font-weight: 650; }
                  .flow-stage-heading span { color: #737e7d; font-size: 10px; }
                  .flow-node { position: relative; margin-bottom: 17px; padding: 17px 16px 16px; border: 1px solid #dce3e1; border-radius: 9px; background: rgba(255,255,255,.96); box-shadow: 0 8px 22px rgba(31,49,51,.055); }
                  .flow-node:last-child { margin-bottom: 0; }
                  .flow-node-top { display: flex; align-items: center; gap: 9px; margin-bottom: 10px; color: #586563; font-size: 9px; }
                  .flow-node-icon { display: grid; flex: 0 0 29px; width: 29px; height: 29px; place-items: center; border: 1px solid #d6e1df; border-radius: 6px; background: #f4f8f7; color: #26758b; font-size: 13px; }
                  .flow-node-top span:last-child { margin-left: auto; }
                  .flow-node h4 { margin: 0 0 6px; color: #202727; font-size: 14px; font-weight: 650; line-height: 1.35; }
                  .flow-node p { margin: 0; color: #687371; font-size: 11px; line-height: 1.55; }
                  .flow-node--question { border-left: 3px solid #e5a64e; }
                  .flow-node--finding { border-left: 3px solid #df7967; }
                  .flow-node--action { border-left: 3px solid #43a5c1; }
                  .flow-node-values { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; margin-top: 13px; }
                  .flow-node-values span { padding: 9px; border-radius: 5px; background: #f3f6f5; color: #626e6c; font-size: 9px; line-height: 1.4; }
                  .flow-node-values strong { display: block; margin-top: 3px; color: #202727; font-size: 12px; }
                  .flow-notice-label { display: flex; align-items: center; gap: 7px; margin-bottom: 9px; color: #ad5546; font-size: 9px; font-weight: 650; }
                  .flow-notice-label span { display: inline-grid; width: 21px; height: 21px; place-items: center; border-radius: 5px; background: #f8e7e2; font-size: 11px; }
                  .flow-board-caption { display: flex; justify-content: space-between; gap: 24px; margin-top: 22px; padding-top: 15px; border-top: 1px solid #dce3e1; color: #727d7b; font-size: 10px; line-height: 1.5; }
                  .flow-board-caption strong { color: #3b4644; font-weight: 600; }
                  .flow-board--motion-ready .flow-node { opacity: 0; transform: translateY(16px); }
                  .flow-board--motion-ready .flow-node.is-visible { opacity: 1; transform: translateY(0); transition: opacity .45s ease, transform .58s cubic-bezier(.2,.7,.15,1); }
                  .flow-node:nth-child(2) { transition-delay: .08s; }
                  .flow-node:nth-child(3) { transition-delay: .16s; }

                  .home-page .home-small-business {
                    width: min(1280px, calc(100% - 104px));
                    grid-template-columns: minmax(0, 1.1fr) minmax(280px, .75fr);
                    margin: 0 auto;
                    padding: clamp(72px, 10vw, 118px) 0;
                    border-color: var(--color-mist);
                  }
                  .home-page .home-small-business h2 { max-width: 700px; font-size: clamp(41px, 5.4vw, 68px); line-height: 1.02; }
                  .home-page .home-small-business > div:last-child { padding-top: 27px; }
                  .home-page .home-small-business > div:last-child > p { color: var(--color-ash); font-size: 14px; line-height: 1.75; }
                  .home-page .home-small-business .small-business-note { padding-left: 13px; border-color: var(--color-signal-blue); font-size: 11px; }
                  .home-page .home-small-business .quiet-link { font-size: 13px; }
                  .home-page .home-close {
                    width: min(1280px, calc(100% - 104px));
                    margin: 0 auto 24px;
                    padding: clamp(34px, 5vw, 62px) 0;
                    border-top: 1px solid var(--color-mist);
                    border-bottom: 1px solid var(--color-mist);
                    border-radius: 0;
                    background: transparent;
                  }
                  .home-page .home-close h2 { max-width: 850px; font-size: clamp(43px, 6vw, 74px); line-height: 1; }
                  .home-page .home-close > p:not(.eyebrow) { color: var(--color-ash); font-size: 14px; line-height: 1.7; }
                  .home-page .home-close .home-primary { margin-top: 23px; border-color: var(--color-ink-black); border-radius: 4px; background: var(--color-ink-black); color: #fff; }
                  .home-page .home-close .home-primary:hover { background: var(--color-twilight); }
                  .home-page .home-footer { width: min(1280px, calc(100% - 104px)); margin: 0 auto; padding: 20px 0 30px; border-color: var(--color-mist); }
                  .home-page .home-footer > span:nth-child(2) { margin-left: auto; }
                  .home-page .home-footer a { color: var(--color-cerulean); font-size: 11px; }
                  .home-page a:focus-visible { outline-color: var(--color-cerulean); }
                  @media (max-width: 760px) {
                    .hero-sequence { min-height: 160svh; }
                    .home-hero { top: 11px; height: calc(100svh - 22px); min-height: min(520px, calc(100svh - 22px)); margin: 11px; border-radius: 23px; }
                    .home-nav { top: 12px; width: calc(100% - 28px); min-height: 53px; padding-left: 15px; }
                    .home-nav nav { gap: 0; }
                    .home-nav nav a { padding: 8px 9px; font-size: 10px; }
                    .home-nav .nav-action { gap: 6px; padding: 9px 10px; font-size: 10px; }
                    .home-brand { font-size: 18px; }
                    .hero-story { bottom: 68px; left: 18px; width: min(485px, calc(100% - 36px)); padding: 21px; }
                    .hero-story h1 { max-width: 430px; font-size: clamp(38px, 8vw, 51px); }
                    .hero-story > p:not(.hero-kicker):not(.hero-truth) { font-size: 12px; }
                    .hero-truth { font-size: 9px; }
                    .hero-notification { top: 90px; right: 18px; width: min(330px, calc(100% - 36px)); padding: 10px; }
                    .notification-copy strong { font-size: 11px; }
                    .notification-copy small { font-size: 9px; }
                    .scroll-cue { right: 23px; bottom: 22px; }
                    .hero-caption { display: none; }
                    .home-growth { width: 100%; padding: 82px 0 60px; }
                    .growth-intro, .growth-disclosure { width: calc(100% - 44px); }
                    .growth-intro { margin-bottom: 32px; }
                    .growth-intro h2 { font-size: clamp(42px, 10vw, 65px); }
                    .growth-intro > p:last-child { font-size: 14px; }
                    .growth-track { min-height: 270svh; }
                    .growth-scene { min-height: 0; }
                    .pixel-tree { right: -17vw; bottom: -6vh; width: 112vw; height: 78vh; opacity: .94; }
                    .growth-step { min-height: 90svh; padding: 35px 0; }
                    .growth-step-inner { width: calc(100% - 44px); margin-left: 22px; padding: 24px; background: rgba(9,67,99,.47); }
                    .growth-step .eyebrow { font-size: 10px; line-height: 1.4; }
                    .growth-step h3 { font-size: clamp(30px, 8vw, 43px); }
                    .growth-step-inner > p:last-child { font-size: 12px; line-height: 1.65; }
                    .growth-counter { right: 22px; bottom: 17px; font-size: 10px; }
                    .growth-disclosure { font-size: 10px; }
                    .discovery-flow { width: calc(100% - 44px); padding: 70px 0; }
                    .discovery-flow-heading { grid-template-columns: 1fr; gap: 15px; margin-bottom: 27px; }
                    .discovery-flow-heading h2 { font-size: clamp(39px, 10vw, 60px); }
                    .discovery-flow-heading > p { max-width: 570px; font-size: 13px; }
                    .flow-board { padding: 21px 17px; border-radius: 12px; background-size: 18px 18px; }
                    .flow-connectors { display: none; }
                    .flow-stages { grid-template-columns: 1fr; gap: 25px; }
                    .flow-stage { padding-left: 17px; border-left: 1px dashed #9fb5b4; }
                    .flow-stage-heading { margin-bottom: 13px; }
                    .flow-node { margin-bottom: 11px; padding: 14px; }
                    .flow-board-caption { flex-direction: column; gap: 6px; }
                    .home-page .discovery-flow, .home-page .home-small-business, .home-page .home-close { width: calc(100% - 44px); }
                    .home-page .home-small-business { grid-template-columns: 1fr; gap: 7px; padding: 67px 0; }
                    .home-page .home-small-business > div:last-child { padding-top: 8px; }
                    .home-page .home-close { padding: 40px 0; }
                    .home-page .home-footer { width: calc(100% - 44px); }
                  }
                  @media (max-width: 420px) {
                    .home-nav { grid-template-columns: auto 1fr auto; }
                    .home-nav nav { justify-content: center; }
                    .home-nav nav a { padding-inline: 6px; font-size: 9px; }
                    .home-nav .nav-action { padding-inline: 8px; font-size: 9px; }
                    .hero-story { bottom: 61px; padding: 19px; }
                    .hero-story h1 { font-size: clamp(40px, 11vw, 52px); }
                    .hero-notification { top: 78px; grid-template-columns: 29px minmax(0, 1fr) 25px; gap: 8px; }
                    .notification-mark { width: 29px; height: 29px; }
                    .notification-open { width: 25px; height: 25px; }
                    .hero-actions { gap: 11px; }
                    .hero-primary { font-size: 11px; }
                    .hero-secondary { font-size: 11px; }
                    .growth-step-inner { padding: 20px; }
                    .growth-step h3 { font-size: clamp(29px, 8vw, 37px); }
                  }
                  @media (prefers-reduced-motion: reduce) {
                    .home-page *, .home-page *::before, .home-page *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; }
                    .hero-landscape, .hero-landscape .city-layer, .hero-story, .hero-notification, .growth-clouds, .growth-sun, .tree-stage, .growth-step-inner, .flow-node { transform: none !important; }
                    .hero-notification { opacity: 1; }
                    .flow-board--motion-ready .flow-node { opacity: 1; }
                  }
                """);
        html.append("""
                  </style>
                </head>
                <body class="home-page">
                  <main class="home-main">
                    <header class="home-nav" aria-label="Main navigation">
                      <a class="home-brand" href="/" aria-label="Kaviyo home">kaviyo</a>
                      <nav aria-label="Page navigation">
                        <a href="#how-kaviyo-helps">How it helps</a>
                        <a href="#discovery-flow">Product flow</a>
                        <a href="/discovery">Discovery workspace</a>
                        <button class="theme-toggle" data-theme-toggle type="button" aria-pressed="false" aria-label="Switch to dark mode">◐</button>
                      </nav>
                      <a class="nav-action" href="/discovery">Check my product page <span aria-hidden="true">↗</span></a>
                    </header>
                    <div class="hero-sequence" data-hero-sequence>
                    <section class="home-hero" aria-labelledby="home-title">
                      <svg class="hero-landscape" viewBox="0 0 1600 980" preserveAspectRatio="xMidYMid slice" role="img" aria-labelledby="landscape-title landscape-description">
                        <title id="landscape-title">A city at sunset seen through a flowering park</title>
                        <desc id="landscape-description">An original Seattle-inspired sunset scene with the Space Needle, Mount Rainier, the waterfront, and cars moving along a road.</desc>
                        <defs>
                          <linearGradient id="hero-sky" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0" stop-color="#332c4d"/>
                            <stop offset=".36" stop-color="#8f4b70"/>
                            <stop offset=".68" stop-color="#ed8067"/>
                            <stop offset="1" stop-color="#f3b56d"/>
                          </linearGradient>
                          <linearGradient id="hero-water" x1="0" y1="0" x2="0" y2="1">
                            <stop stop-color="#564768"/>
                            <stop offset="1" stop-color="#292d40"/>
                          </linearGradient>
                          <linearGradient id="hero-hill" x1="0" y1="0" x2="1" y2="1">
                            <stop stop-color="#a75b69"/>
                            <stop offset="1" stop-color="#4b4057"/>
                          </linearGradient>
                          <radialGradient id="hero-sun">
                            <stop stop-color="#ffe3a0" stop-opacity=".95"/>
                            <stop offset=".45" stop-color="#ffc77d" stop-opacity=".48"/>
                            <stop offset="1" stop-color="#f79882" stop-opacity="0"/>
                          </radialGradient>
                          <filter id="hero-soft"><feGaussianBlur stdDeviation="28"/></filter>
                        </defs>
                        <rect width="1600" height="980" fill="url(#hero-sky)"/>
                        <ellipse cx="1080" cy="430" rx="530" ry="280" fill="url(#hero-sun)"/>
                        <circle cx="1080" cy="430" r="98" fill="#ffd187" opacity=".7"/>
                        <path d="M748 476 884 395l69 38 105-161 118 147 92-83 151 140H748Z" fill="#79536e" opacity=".48"/>
                        <path d="m981 337 77-65 48 69-37-21-27 24-23-18-22 20Z" fill="#f4d9c5" opacity=".55"/>
                        <path d="M0 470 C250 420 390 456 610 430 S1060 460 1300 422 1515 420 1600 444 V650 H0Z" fill="#9b5368" opacity=".34"/>
                        <g class="city-layer city-layer--distant">
                          <g fill="#342f48">
                            <path d="M63 576V365h68v-38h21v38h18v211z"/>
                            <path d="M470 581V309h51v-23h18v23h43v272z"/>
                            <path d="M607 573V392h88v-34h22v34h49v181z"/>
                            <path d="M1012 579V350h70v-44h24v44h44v229z"/>
                            <path d="M1391 577V345h76v-34h22v34h62v232z"/>
                          </g>
                          <g fill="#f3bc81" opacity=".6">
                            <path d="M90 398h8v11h-8zm25 0h8v11h-8zm-25 32h8v11h-8zm25 0h8v11h-8z"/>
                            <path d="M489 348h8v12h-8zm22 0h8v12h-8zm-22 30h8v12h-8zm22 0h8v12h-8z"/>
                            <path d="M1412 384h9v12h-9zm25 0h9v12h-9zm-25 30h9v12h-9zm25 0h9v12h-9z"/>
                          </g>
                        </g>
                        <g class="city-layer city-layer--front">
                          <g fill="#342f48">
                            <path d="M197 578V290h82v-30h18v30h53v288z"/>
                            <path d="M335 574V381h74v-50h22v50h68v193z"/>
                            <path d="M813 578V274h118v-28h24v28h39v304z"/>
                            <path d="M1176 573V250h89v-41h19v41h72v323z"/>
                          </g>
                          <g fill="#f3bc81" opacity=".6">
                            <path d="M220 320h9v12h-9zm25 0h9v12h-9zm-25 32h9v12h-9zm25 0h9v12h-9zm-25 32h9v12h-9zm25 0h9v12h-9z"/>
                            <path d="M832 300h10v14h-10zm30 0h10v14h-10zm-30 38h10v14h-10zm30 0h10v14h-10zm-30 38h10v14h-10zm30 0h10v14h-10z"/>
                            <path d="M1201 278h10v13h-10zm34 0h10v13h-10zm-34 38h10v13h-10zm34 0h10v13h-10zm-34 38h10v13h-10zm34 0h10v13h-10z"/>
                          </g>
                        </g>
                        <g class="city-layer city-layer--front" fill="#292d43" stroke="#292d43" stroke-linejoin="round">
                          <path d="M849 255 852 166l4 89z"/>
                          <path d="M843 250h18v23h-18z"/>
                          <path d="M817 273q34-19 68 0l15 9-3 12q-45 14-92 0l-3-12z"/>
                          <path d="M814 294q70 18 142 0l-7 12q-63 19-128 0z"/>
                          <path d="M841 305h22l-4 124-7 47-11-47z"/>
                          <path d="m842 324-14 109-47 148h17l55-135 7-122zm19 0 14 109 48 148h-18l-54-135-7-122z"/>
                          <path d="M839 319h27v13h-27z"/>
                        </g>
                        <g class="city-layer city-layer--distant" fill="none" stroke="#e9c5a0" stroke-width="4" opacity=".72">
                          <circle cx="485" cy="551" r="53"/>
                          <path d="M485 498v106m-53-53h106m-91-38 76 76m0-76-76 76M462 601l-18 35m64-35 19 35"/>
                          <circle cx="485" cy="551" r="6" fill="#e9c5a0"/>
                        </g>
                        <path d="M0 610 C230 574 359 608 520 572 S823 555 1000 593 1330 563 1600 602 V725 H0Z" fill="url(#hero-hill)"/>
                        <path d="M0 698 C258 640 449 689 662 648 S1060 674 1231 636 1477 655 1600 632 V980 H0Z" fill="url(#hero-water)"/>
                        <g fill="none" stroke-linecap="round" opacity=".45">
                          <path d="M510 720h250m130 17h352M135 764h320m766 16h214M473 830h460m255 33h225M172 896h300m294 28h605" stroke="#e7ad89" stroke-width="5"/>
                          <path d="M925 757h140m-374 88h225m483-168h118M54 850h205m750 96h180" stroke="#a58b9a" stroke-width="3"/>
                        </g>
                        <path d="M0 719 C148 632 276 641 397 727 C473 782 529 790 606 772 C671 756 719 684 800 690 C895 697 930 806 1015 822 C1103 838 1145 744 1250 735 C1378 723 1463 815 1600 733 V980 H0Z" fill="#292d3b"/>
                        <g fill="#415544">
                          <circle cx="90" cy="644" r="120"/><circle cx="204" cy="638" r="112"/><circle cx="302" cy="674" r="104"/>
                          <circle cx="1365" cy="654" r="124"/><circle cx="1496" cy="664" r="140"/><circle cx="1577" cy="624" r="115"/>
                        </g>
                        <g fill="#bc657d" opacity=".92">
                          <circle cx="55" cy="617" r="20"/><circle cx="127" cy="572" r="16"/><circle cx="217" cy="616" r="21"/>
                          <circle cx="278" cy="651" r="15"/><circle cx="1375" cy="594" r="19"/><circle cx="1460" cy="617" r="23"/>
                          <circle cx="1535" cy="567" r="17"/><circle cx="1584" cy="626" r="21"/>
                        </g>
                        <g fill="#edafaa">
                          <circle cx="52" cy="598" r="11"/><circle cx="86" cy="649" r="9"/><circle cx="149" cy="555" r="10"/>
                          <circle cx="228" cy="601" r="12"/><circle cx="268" cy="684" r="10"/><circle cx="1403" cy="584" r="11"/>
                          <circle cx="1478" cy="637" r="10"/><circle cx="1551" cy="550" r="11"/><circle cx="1587" cy="657" r="10"/>
                        </g>
                        <g fill="none" stroke="#312d43" stroke-linecap="round">
                          <path d="M-40 20C76 96 106 176 119 270s-23 174-98 248M-2 174c73-37 135-43 199-29M22 255c68-10 118-37 165-82M1590 0c-95 86-113 166-98 264s47 164 110 224M1567 147c-65-29-123-28-182-9m165 108c-59-3-108-27-158-72" stroke-width="20"/>
                          <path d="M15 192c-52-44-61-88-61-128m180 58c22-48 62-68 101-80m1287 15c-14-47-50-70-91-91m-27 178c-27-55-69-78-112-90" stroke-width="11"/>
                        </g>
                        <g fill="#edb6b3" opacity=".95">
                          <circle cx="71" cy="117" r="17"/><circle cx="95" cy="130" r="14"/><circle cx="70" cy="145" r="12"/>
                          <circle cx="179" cy="103" r="13"/><circle cx="201" cy="113" r="18"/><circle cx="186" cy="132" r="12"/>
                          <circle cx="1491" cy="118" r="16"/><circle cx="1513" cy="130" r="13"/><circle cx="1489" cy="148" r="12"/>
                          <circle cx="1390" cy="116" r="15"/><circle cx="1411" cy="126" r="17"/><circle cx="1402" cy="148" r="12"/>
                        </g>
                        <g fill="#fff0d6" opacity=".92">
                          <circle cx="80" cy="124" r="5"/><circle cx="193" cy="108" r="5"/><circle cx="1500" cy="125" r="5"/><circle cx="1401" cy="121" r="5"/>
                        </g>
                        <path d="M-70 831c233-76 450-63 657-20 211 44 398 102 632 61 182-32 305-104 460-108" fill="none" stroke="#202b3a" stroke-width="134"/>
                        <path d="M-70 831c233-76 450-63 657-20 211 44 398 102 632 61 182-32 305-104 460-108" fill="none" stroke="#b6a996" stroke-width="3" opacity=".68"/>
                        <path d="M-70 831c233-76 450-63 657-20 211 44 398 102 632 61 182-32 305-104 460-108" fill="none" stroke="#f3d796" stroke-width="5" stroke-dasharray="34 30" opacity=".85"/>
                        <g class="traffic-flow traffic-flow--east" data-traffic-east fill="none" stroke-linejoin="round" transform="translate(0 -30)" aria-hidden="true">
                          <g transform="translate(135 789)">
                            <path d="M0 24 10 9h27L48 0h37l15 13h16l10 11v17H0z" fill="#f5bd70"/>
                            <path d="M43 8 37 19h51L77 8z" fill="#9b5368"/>
                            <circle cx="27" cy="41" r="8" fill="#20232e"/><circle cx="103" cy="41" r="8" fill="#20232e"/>
                          </g>
                          <g transform="translate(690 823)">
                            <path d="M0 24 10 9h27L48 0h37l15 13h16l10 11v17H0z" fill="#58b6c9"/>
                            <path d="M43 8 37 19h51L77 8z" fill="#41566e"/>
                            <circle cx="27" cy="41" r="8" fill="#20232e"/><circle cx="103" cy="41" r="8" fill="#20232e"/>
                          </g>
                          <g transform="translate(1210 835)">
                            <path d="M0 24 10 9h27L48 0h37l15 13h16l10 11v17H0z" fill="#ed8067"/>
                            <path d="M43 8 37 19h51L77 8z" fill="#70465c"/>
                            <circle cx="27" cy="41" r="8" fill="#20232e"/><circle cx="103" cy="41" r="8" fill="#20232e"/>
                          </g>
                        </g>
                        <g class="traffic-flow traffic-flow--west" data-traffic-west fill="none" stroke-linejoin="round" transform="translate(0 30)" aria-hidden="true">
                          <g transform="translate(540 805)"><g transform="translate(120 0) scale(-1 1)">
                            <path d="M0 24 10 9h27L48 0h37l15 13h16l10 11v17H0z" fill="#f5d36e"/>
                            <path d="M43 8 37 19h51L77 8z" fill="#5d5577"/>
                            <circle cx="27" cy="41" r="8" fill="#20232e"/><circle cx="103" cy="41" r="8" fill="#20232e"/>
                          </g></g>
                          <g transform="translate(1050 845)"><g transform="translate(120 0) scale(-1 1)">
                            <path d="M0 24 10 9h27L48 0h37l15 13h16l10 11v17H0z" fill="#e9a4aa"/>
                            <path d="M43 8 37 19h51L77 8z" fill="#70465c"/>
                            <circle cx="27" cy="41" r="8" fill="#20232e"/><circle cx="103" cy="41" r="8" fill="#20232e"/>
                          </g></g>
                        </g>
                      </svg>
                      <div class="hero-veil" aria-hidden="true"></div>
                      <div class="hero-story">
                      <p class="hero-kicker">AI is changing how shoppers compare products.</p>
                      <h1 id="home-title">Help AI agents understand where your products fit.</h1>
                        <p>Shopping agents shape how buyers compare products. Kaviyo helps your team bring in a catalog, connect product facts to real buyer needs, and see what still needs evidence.</p>
                        <div class="hero-actions">
                          <a class="hero-primary" href="#how-kaviyo-helps">See how Kaviyo helps <span aria-hidden="true">↓</span></a>
                          <a class="hero-secondary" href="/discovery">Check a product page</a>
                        </div>
                        <p class="hero-truth">Clear details help people compare. Shoppers and assistants make the final choice.</p>
                      </div>
                      <aside class="hero-notification" aria-label="Sample product finding">
                        <span class="notification-mark" aria-hidden="true">↕</span>
                        <span class="notification-copy"><span class="notification-label">Synthetic discovery opportunity</span><strong>A shopper need is not yet supported by clear evidence</strong><small>Demo daypack · fit for a 15-inch laptop is not established</small></span>
                        <a class="notification-open" href="/audit/start" aria-label="Open the sample audit">↗</a>
                      </aside>
                      <a class="scroll-cue" href="#how-kaviyo-helps"><span aria-hidden="true"></span>Keep going</a>
                      <span class="hero-caption">A clearer path from product facts to discovery</span>
                    </section>
                    </div>

                    <section id="how-kaviyo-helps" class="home-growth" aria-labelledby="growth-title">
                      <div class="growth-intro">
                        <p class="eyebrow">Make the value easier to see</p>
                        <h2 id="growth-title">Help your product make its case.</h2>
                        <p>Agents choose their own recommendations. Merchants can still make products easier to evaluate by keeping fit-critical facts clear, consistent, and supported by evidence. Kaviyo helps teams see where a product matches a mission and where uncertainty remains.</p>
                      </div>
                      <div class="growth-track" data-growth-track>
                        <div class="growth-scene" data-scroll-story data-growth="1" aria-hidden="true">
                          <svg class="growth-sky" viewBox="0 0 1600 1000" preserveAspectRatio="xMidYMid slice">
                            <defs>
                              <linearGradient id="growth-sky-color" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#0081c0"/><stop offset=".66" stop-color="#48b6df"/><stop offset="1" stop-color="#b5e8df"/></linearGradient>
                              <linearGradient id="growth-ground-color" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#4a9e65"/><stop offset="1" stop-color="#1c674e"/></linearGradient>
                            </defs>
                            <rect width="1600" height="1000" fill="url(#growth-sky-color)"/>
                            <g class="growth-sun"><circle cx="420" cy="210" r="142" fill="#f5e211"/><circle cx="420" cy="210" r="188" fill="#f5e211" opacity=".12"/></g>
                            <g class="growth-clouds" fill="#d7f3ef" opacity=".85">
                              <path d="M85 266h196v-28h56v28h56v52H85zm430-113h150v-25h40v25h59v48H515zm666 141h110v-32h48v32h55v48h-213z"/>
                              <path d="M185 344h130v22H185zm416-103h102v22H601zm690 118h111v22h-111z" opacity=".55"/>
                            </g>
                            <path d="M0 700c180-90 337-45 495 5 168 52 284 29 438-28 205-77 400-55 667 44v279H0z" fill="#56ad72"/>
                            <path d="M0 810c201-78 396-16 581 36 174 50 359 39 546-15 188-54 307-33 473 4v165H0z" fill="url(#growth-ground-color)"/>
                            <g fill="#fff1a4" opacity=".9"><rect x="116" y="816" width="12" height="12"/><rect x="178" y="888" width="10" height="10"/><rect x="470" y="846" width="12" height="12"/><rect x="1350" y="840" width="12" height="12"/><rect x="1464" y="900" width="10" height="10"/></g>
                            <g class="tree-stage tree-stage-one">
                              <rect x="1068" y="656" width="24" height="258" fill="#23734e"/><path d="M1080 806l-92-62 17-22 91 48zm0 49 102-61-17-23-97 48z" fill="#3b9a58"/>
                              <g fill="#ffd82f"><rect x="1060" y="535" width="28" height="54"/><rect x="1094" y="535" width="28" height="54"/><rect x="1018" y="552" width="42" height="48"/><rect x="1120" y="552" width="42" height="48"/><rect x="1034" y="594" width="34" height="32"/><rect x="1100" y="594" width="34" height="32"/></g>
                              <rect x="1050" y="567" width="83" height="58" fill="#9b4d27"/><rect x="1062" y="578" width="60" height="37" fill="#683b2d"/><rect x="1075" y="587" width="32" height="19" fill="#2e3533"/>
                              <rect x="1020" y="738" width="40" height="23" fill="#90db70"/><rect x="1110" y="773" width="42" height="22" fill="#54b85f"/>
                            </g>
                            <g class="tree-stage tree-stage-two">
                              <rect x="1080" y="493" width="24" height="423" fill="#23734e"/><path d="M1092 724l-134-90 20-22 132 73zm0 88 144-90-18-23-140 75z" fill="#3b9a58"/><path d="M958 630h46v34h-46zm-25-20h42v30h-42zm301 69h-48v34h48zm26-20h-42v30h42z" fill="#79cf6e"/>
                              <g fill="#ffd82f"><rect x="1060" y="305" width="28" height="74"/><rect x="1094" y="305" width="28" height="74"/><rect x="995" y="332" width="42" height="62"/><rect x="1145" y="332" width="42" height="62"/><rect x="954" y="382" width="58" height="42"/><rect x="1170" y="382" width="58" height="42"/><rect x="965" y="431" width="52" height="40"/><rect x="1165" y="431" width="52" height="40"/><rect x="1002" y="474" width="40" height="43"/><rect x="1138" y="474" width="40" height="43"/></g>
                              <g fill="#f39b25"><rect x="1088" y="327" width="28" height="52"/><rect x="1010" y="350" width="28" height="44"/><rect x="1145" y="350" width="28" height="44"/><rect x="978" y="390" width="34" height="32"/><rect x="1170" y="390" width="34" height="32"/><rect x="995" y="440" width="22" height="30"/><rect x="1165" y="440" width="22" height="30"/></g>
                              <rect x="1023" y="375" width="156" height="100" fill="#9b4d27"/><rect x="1045" y="393" width="116" height="64" fill="#683b2d"/><rect x="1073" y="411" width="60" height="30" fill="#2e3533"/>
                              <rect x="1058" y="582" width="42" height="24" fill="#90db70"/><rect x="1100" y="560" width="39" height="24" fill="#54b85f"/><rect x="1038" y="786" width="42" height="24" fill="#54b85f"/><rect x="1104" y="758" width="50" height="25" fill="#90db70"/>
                            </g>
                            <g class="tree-stage tree-stage-three">
                              <g transform="translate(-335 230) scale(.67)">
                                <rect x="1080" y="493" width="24" height="423" fill="#23734e"/><path d="M1092 724l-134-90 20-22 132 73zm0 88 144-90-18-23-140 75z" fill="#3b9a58"/>
                                <g fill="#ffd82f"><rect x="1060" y="305" width="28" height="74"/><rect x="1094" y="305" width="28" height="74"/><rect x="995" y="332" width="42" height="62"/><rect x="1145" y="332" width="42" height="62"/><rect x="954" y="382" width="58" height="42"/><rect x="1170" y="382" width="58" height="42"/><rect x="965" y="431" width="52" height="40"/><rect x="1165" y="431" width="52" height="40"/><rect x="1002" y="474" width="40" height="43"/><rect x="1138" y="474" width="40" height="43"/></g>
                                <rect x="1023" y="375" width="156" height="100" fill="#9b4d27"/><rect x="1045" y="393" width="116" height="64" fill="#683b2d"/><rect x="1073" y="411" width="60" height="30" fill="#2e3533"/>
                              </g>
                              <g fill="#f5e211"><rect x="1350" y="458" width="22" height="66"/><rect x="1378" y="458" width="22" height="66"/><rect x="1310" y="488" width="38" height="48"/><rect x="1402" y="488" width="38" height="48"/><rect x="1333" y="532" width="34" height="38"/><rect x="1380" y="532" width="34" height="38"/></g>
                              <rect x="1344" y="505" width="65" height="54" fill="#9b4d27"/><rect x="1355" y="516" width="44" height="32" fill="#2e3533"/><rect x="1365" y="570" width="20" height="307" fill="#23734e"/>
                            </g>
                          </svg>
                          <div class="growth-counter" aria-hidden="true">01 <span>/</span> 03</div>
                        </div>
                        <div class="growth-steps" aria-label="How Kaviyo helps product discovery">
                          <article class="growth-step is-active" data-journey-step="1" aria-current="step">
                            <div class="growth-step-inner"><p class="eyebrow">Start with the buyer</p><h3>Show why your product fits.</h3><p>When people compare options, specific details help them see whether your product meets their needs.</p></div>
                          </article>
                          <article class="growth-step" data-journey-step="2" aria-hidden="true">
                            <div class="growth-step-inner"><p class="eyebrow">Find what is getting in the way</p><h3>Catch the details that weaken the story.</h3><p>Kaviyo checks the product information you control and flags missing or conflicting details for your team to review.</p></div>
                          </article>
                          <article class="growth-step" data-journey-step="3" aria-hidden="true">
                            <div class="growth-step-inner"><p class="eyebrow">Turn insight into action</p><h3>Make the next fix clear.</h3><p>Owners get a plain-language review. Developers get the source and field details they need when a change requires code.</p></div>
                          </article>
                        </div>
                      </div>
                      <p class="growth-disclosure">This walkthrough uses fictional sample products and saved page evidence. The demo does not scan a live store or measure third-party assistant rankings.</p>
                    </section>

                    <section id="discovery-flow" class="discovery-flow" aria-labelledby="discovery-flow-title">
                      <div class="discovery-flow-heading">
                        <div><p class="eyebrow">The problem Kaviyo is built to solve</p><h2 id="discovery-flow-title">Help AI understand why your product fits.</h2></div>
                        <p>Assistants can only compare product facts they can find. Kaviyo helps your team spot missing or inconsistent specs, then decide what to make clearer.</p>
                      </div>
                      <div class="flow-board" data-flow-board>
                        <svg class="flow-connectors" viewBox="0 0 1200 560" preserveAspectRatio="none" aria-hidden="true">
                          <path d="M332 164 C378 164 384 154 428 154 M332 260 C378 260 384 351 428 351 M760 154 C811 154 817 154 868 154 M760 351 C811 351 817 351 868 351"/>
                        </svg>
                        <div class="flow-stages" aria-label="Illustrative product discovery flow">
                          <section class="flow-stage" aria-labelledby="flow-stage-discovery">
                            <header class="flow-stage-heading"><h3 id="flow-stage-discovery">A shopper asks AI</h3><span>Buyer intent</span></header>
                            <article class="flow-node flow-node--question" data-flow-node>
                              <div class="flow-node-top"><span class="flow-node-icon" aria-hidden="true">⌕</span><strong>Shopping question</strong><span>Example</span></div>
                              <h4>“Which laptop has 32 GB RAM, an OLED screen, and long battery life for editing?”</h4>
                              <p>The buyer describes the job to do and the product specs that matter.</p>
                            </article>
                            <article class="flow-node" data-flow-node>
                              <div class="flow-node-top"><span class="flow-node-icon" aria-hidden="true">↔</span><strong>Assistant comparison</strong></div>
                              <h4>The assistant builds its own shortlist.</h4>
                              <p>It may use product pages, catalogs, and other sources it can access. The merchant does not control its choice.</p>
                            </article>
                          </section>
                          <section class="flow-stage" aria-labelledby="flow-stage-kaviyo">
                            <header class="flow-stage-heading"><h3 id="flow-stage-kaviyo">Kaviyo finds the gaps</h3><span>Product readiness</span></header>
                            <article class="flow-node" data-flow-node>
                              <div class="flow-node-top"><span class="flow-node-icon" aria-hidden="true">✳</span><strong>Turn intent into checks</strong></div>
                              <h4>Translate the question into specs.</h4>
                              <p>Muse Spark can identify the requested attributes. Deterministic checks compare them with merchant-approved product facts.</p>
                            </article>
                            <article class="flow-node flow-node--finding" data-flow-node>
                              <div class="flow-notice-label"><span aria-hidden="true">!</span>Synthetic example finding</div>
                              <h4>32 GB memory is missing from the product page.</h4>
                              <p>The sample catalog has a value; the saved product page does not state it.</p>
                              <div class="flow-node-values"><span>Catalog<strong>32 GB RAM</strong></span><span>Product page<strong>Not stated</strong></span></div>
                            </article>
                          </section>
                          <section class="flow-stage" aria-labelledby="flow-stage-team">
                            <header class="flow-stage-heading"><h3 id="flow-stage-team">The business improves its facts</h3><span>Human-approved changes</span></header>
                            <article class="flow-node flow-node--action" data-flow-node>
                              <div class="flow-node-top"><span class="flow-node-icon" aria-hidden="true">✓</span><strong>Confirm product specs</strong></div>
                              <h4>The owner verifies the evidence.</h4>
                              <p>Kaviyo suggests what to clarify; the merchant confirms every product fact before making a change.</p>
                            </article>
                            <article class="flow-node" data-flow-node>
                              <div class="flow-node-top"><span class="flow-node-icon" aria-hidden="true">↗</span><strong>Publish and re-check</strong></div>
                              <h4>Make the verified detail easier to find.</h4>
                              <p>Update product pages and structured data, then repeat the same controlled checks. Measure real outcomes when channel analytics allow.</p>
                            </article>
                          </section>
                        </div>
                        <div class="flow-board-caption"><strong>Complete, consistent specs give assistants clearer facts to evaluate.</strong><span>This is a planned workflow shown with synthetic data. Better information cannot guarantee a recommendation or ranking.</span></div>
                      </div>
                    </section>

                    <section class="home-small-business" aria-labelledby="small-team-title">
                      <div>
                        <p class="eyebrow">Made for small teams</p>
                        <h2 id="small-team-title">Give every teammate a clear next move.</h2>
                      </div>
                      <div>
                        <p>Store owners see what needs attention in plain language. Developers get the source evidence and exact field when a fix needs code.</p>
                        <p class="small-business-note">In this prototype, the guided audit and evidence report work with fictional data. Store connections, AI simulations, and publishing are not connected yet.</p>
                        <a class="quiet-link" href="/how-it-works">Read the product workflow</a>
                      </div>
                    </section>

                    <section class="home-close" aria-labelledby="close-title">
                      <p class="eyebrow">Start with your business</p>
                      <h2 id="close-title">Make product fit easier for agents to understand.</h2>
                      <p>Bring in your catalog, choose a buyer need, and explore a controlled agent simulation.</p>
                      <a class="action-link home-primary" href="/discovery">Set up your business <span aria-hidden="true">↗</span></a>
                    </section>
                  </main>
                  <footer class="footer home-footer"><span>Kaviyo</span><span>Sample walkthrough uses synthetic data</span><a href="/how-it-works">How it works</a></footer>
                  <script src="/assets/home-scroll-story.js" defer></script>
                  <script src="/assets/theme.js" defer></script>
                </body>
                </html>
                """);
        return html.toString();
    }

    public String scrollStoryScript() {
        return """
                (() => {
                  const scene = document.querySelector('[data-scroll-story]');
                  const track = document.querySelector('[data-growth-track]');
                  const heroTrack = document.querySelector('[data-hero-sequence]');
                  const hero = document.querySelector('.home-hero');
                  const city = document.querySelector('.hero-landscape');
                  const trafficEast = document.querySelector('[data-traffic-east]');
                  const trafficWest = document.querySelector('[data-traffic-west]');
                  const flowBoard = document.querySelector('[data-flow-board]');
                  const steps = [...document.querySelectorAll('[data-journey-step]')];
                  const counter = document.querySelector('.growth-counter');
                  if (!scene || !track || !heroTrack || !hero || !city || !steps.length) return;

                  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
                  let framePending = false;
                  let currentStep = null;

                  const setActive = (step) => {
                    if (step === currentStep) return;
                    currentStep = step;
                    scene.dataset.growth = step.dataset.journeyStep;
                    if (counter) counter.innerHTML = `${step.dataset.journeyStep.padStart(2, '0')} <span>/</span> ${String(steps.length).padStart(2, '0')}`;
                    steps.forEach((item) => {
                      const isActive = item === step;
                      item.classList.toggle('is-active', isActive);
                      if (isActive) item.setAttribute('aria-current', 'step');
                      else item.removeAttribute('aria-current');
                      if (isActive) item.removeAttribute('aria-hidden');
                      else item.setAttribute('aria-hidden', 'true');
                    });
                  };

                  const clamp = (value) => Math.max(0, Math.min(1, value));
                  const update = () => {
                    framePending = false;
                    const viewportHeight = window.innerHeight || 1;
                    const heroBounds = heroTrack.getBoundingClientRect();
                    const heroTravel = Math.max(1, heroTrack.offsetHeight - viewportHeight);
                    const heroProgress = clamp(-heroBounds.top / heroTravel);
                    const trackBounds = track.getBoundingClientRect();
                    const trackTravel = Math.max(1, track.offsetHeight - viewportHeight);
                    const progress = clamp(-trackBounds.top / trackTravel);
                    const center = viewportHeight * .52;
                    const active = steps.find((step) => {
                      const bounds = step.getBoundingClientRect();
                      return bounds.top <= center && bounds.bottom > center;
                    });

                    let flowProgress = 0;
                    if (flowBoard) {
                      const flowBounds = flowBoard.getBoundingClientRect();
                      flowProgress = clamp((viewportHeight - flowBounds.top) / (viewportHeight + flowBounds.height));
                    }

                    // Keep scroll work in two phases: finish all layout reads before changing styles.
                    if (!reducedMotion) {
                      hero.style.setProperty('--hero-scale', String(1 + heroProgress * .105));
                      hero.style.setProperty('--hero-shift', `${heroProgress * -22}px`);
                      hero.style.setProperty('--hero-copy-opacity', String(1 - heroProgress * .22));
                      hero.style.setProperty('--hero-copy-shift', `${heroProgress * -26}px`);
                      city.style.setProperty('--city-distant-x', `${heroProgress * 4}px`);
                      city.style.setProperty('--city-distant-y', `${heroProgress * -5}px`);
                      city.style.setProperty('--city-front-x', `${heroProgress * -42}px`);
                      city.style.setProperty('--city-front-y', `${heroProgress * -20}px`);
                      city.style.setProperty('--city-front-scale', String(1 + heroProgress * .035));
                      if (trafficEast) trafficEast.setAttribute('transform', `translate(${Math.round(heroProgress * 460)} -30)`);
                      if (trafficWest) trafficWest.setAttribute('transform', `translate(${Math.round(heroProgress * -460)} 30)`);
                      scene.style.setProperty('--cloud-shift', `${progress * -65}px`);
                      scene.style.setProperty('--sun-shift', `${progress * 23}px`);
                      if (flowBoard) {
                        flowBoard.style.setProperty('--flow-grid-x', `${flowProgress * 18}px`);
                        flowBoard.style.setProperty('--flow-grid-y', `${flowProgress * -16}px`);
                      }
                    }
                    if (active) setActive(active);
                  };

                  const requestUpdate = () => {
                    if (framePending) return;
                    framePending = true;
                    window.requestAnimationFrame(update);
                  };
                  window.addEventListener('scroll', requestUpdate, { passive: true });
                  window.addEventListener('resize', requestUpdate, { passive: true });
                  if (flowBoard && !reducedMotion && 'IntersectionObserver' in window) {
                    flowBoard.classList.add('flow-board--motion-ready');
                    const revealObserver = new IntersectionObserver((entries, observer) => {
                      entries.forEach((entry) => {
                        if (!entry.isIntersecting) return;
                        entry.target.classList.add('is-visible');
                        observer.unobserve(entry.target);
                      });
                    }, { rootMargin: '0px 0px -8% 0px', threshold: .12 });
                    flowBoard.querySelectorAll('[data-flow-node]').forEach((node) => revealObserver.observe(node));
                  }
                  update();
                })();
                """;
    }
}
